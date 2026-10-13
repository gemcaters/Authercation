package net.auther.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.services.MinecraftServicesSessionService;
import com.mojang.authlib.services.ProfileResult;
import com.mojang.authlib.services.response.HasJoinedMinecraftServerResponse;
import com.mojang.authlib.services.response.NameAndId;
import net.auther.auth.AuthProvider;
import net.auther.auth.ProviderUuids;
import net.auther.server.AuthSessionHandler;
import net.auther.server.NamePinStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Hooks the server-side {@code hasJoinedServer} call made by the vanilla login
 * handshake.
 *
 * <p>Vanilla asks Mojang's session server "has this player joined with this
 * serverId?" and kicks the player with "Failed to verify username!" when the
 * answer is no. That check happens <em>before</em> any later login hook, so
 * Ely.by players can never get past it unless we intervene here.
 *
 * <p>Flow (server stays on {@code online-mode=true}):
 * <ol>
 *   <li>Run the original Mojang check. Premium players are accepted exactly as
 *       vanilla would accept them.</li>
 *   <li>If Mojang does not know the session, try each configured fallback
 *       provider (Ely.by) in order and build the profile from its response.</li>
 *   <li>If everyone rejects the session, return {@code null} so vanilla
 *       disconnects the player.</li>
 * </ol>
 */
@Mixin(MinecraftServicesSessionService.class)
public abstract class HasJoinedServerMixin {

    @Unique
    private static final Logger LOGGER = LoggerFactory.getLogger("Authercation/Login");

    @Unique
    private static final String MOJANG_LOOKUP_URL =
        "https://api.minecraftservices.com/minecraft/profile/lookup/name/";

    @Shadow
    @Final
    private MinecraftClient client;

    @WrapMethod(method = "hasJoinedServer", remap = false)
    private ProfileResult authercation$hasJoinedServer(
        String profileName,
        String serverId,
        InetAddress address,
        Operation<ProfileResult> original
    ) throws AuthenticationUnavailableException {
        // 1) Premium / Microsoft accounts: vanilla Mojang check.
        AuthenticationUnavailableException mojangUnavailable = null;
        try {
            ProfileResult mojangResult = original.call(profileName, serverId, address);
            if (mojangResult != null) {
                return authercation$pin(profileName, mojangResult);
            }
        } catch (Exception e) {
            // Operation#call declares no checked exceptions, so match on type.
            if (e instanceof AuthenticationUnavailableException unavailable) {
                mojangUnavailable = unavailable;
            } else if (e instanceof RuntimeException runtime) {
                throw runtime;
            } else {
                throw new IllegalStateException(e);
            }
        }

        // 2) Fallback providers (Ely.by, ...).
        List<AuthProvider> providers = AuthSessionHandler.INSTANCE.getFallbackProviders();

        if (!providers.isEmpty()
            && AuthSessionHandler.INSTANCE.getConfig().getPreventFallbackIfPlayerExists()
            && authercation$existsOnMojang(profileName)) {
            LOGGER.warn(
                "Player '{}' exists on Mojang but failed Mojang authentication, fallback providers skipped",
                profileName
            );
            return null;
        }

        for (AuthProvider provider : providers) {
            try {
                ProfileResult result = authercation$tryProvider(provider, profileName, serverId, address);
                if (result != null) {
                    LOGGER.info("Authenticated {} via {}", profileName, provider.getDisplayName());
                    return authercation$pin(profileName, result);
                }
                LOGGER.debug("{} did not recognise session of {}", provider.getDisplayName(), profileName);
            } catch (MinecraftClientException e) {
                LOGGER.warn(
                    "{} session check failed for {}: {}",
                    provider.getDisplayName(), profileName, e.getMessage()
                );
            }
        }

        // 3) Nobody accepted. If Mojang was down, surface that like vanilla would.
        if (mojangUnavailable != null) {
            throw mojangUnavailable;
        }
        return null;
    }

    @Unique
    private ProfileResult authercation$tryProvider(
        AuthProvider provider,
        String profileName,
        String serverId,
        InetAddress address
    ) {
        Map<String, String> arguments = new LinkedHashMap<>();
        arguments.put("username", profileName);
        arguments.put("serverId", serverId);
        if (address != null) {
            arguments.put("ip", address.getHostAddress());
        }

        URL url = authercation$url(provider.getCheckUrl() + authercation$query(provider.getCheckUrl(), arguments));

        LOGGER.debug("Checking {} against {}", profileName, provider.getCheckUrl());

        HasJoinedMinecraftServerResponse response =
            client.get(url, HasJoinedMinecraftServerResponse.class);

        if (response == null || response.id() == null) {
            return null;
        }

        PropertyMap properties = authercation$resolveProperties(provider, profileName, response);

        UUID mappedId = ProviderUuids.map(provider.getId(), response.id());
        GameProfile profile = properties != null
            ? new GameProfile(mappedId, profileName, properties)
            : new GameProfile(mappedId, profileName);

        return new ProfileResult(profile, Collections.emptySet());
    }

    /**
     * Ely.by signs textures with its own key through a dedicated skin system;
     * prefer that over the (often unsigned) properties of the session response.
     */
    @Unique
    private PropertyMap authercation$resolveProperties(
        AuthProvider provider,
        String profileName,
        HasJoinedMinecraftServerResponse response
    ) {
        PropertyMap fallback = response.properties();
        String template = provider.getPropertyUrl();
        if (template == null) {
            return fallback;
        }

        try {
            URL propertyUrl = authercation$url(
                template.replace("{0}", URLEncoder.encode(profileName, StandardCharsets.UTF_8))
            );
            HasJoinedMinecraftServerResponse propertyResponse =
                client.get(propertyUrl, HasJoinedMinecraftServerResponse.class);
            if (propertyResponse != null && propertyResponse.properties() != null) {
                return propertyResponse.properties();
            }
        } catch (MinecraftClientException e) {
            LOGGER.debug("Property lookup failed for {} on {}: {}", profileName, provider.getDisplayName(), e.getMessage());
        }
        return fallback;
    }

    @Unique
    private static ProfileResult authercation$pin(String profileName, ProfileResult result) {
        if (!AuthSessionHandler.INSTANCE.getConfig().isPinNames()) {
            return result;
        }
        GameProfile profile = result.profile();
        if (NamePinStore.claim(profileName, profile.id())) {
            return result;
        }
        LOGGER.warn(
            "Refused {} ({}): the name already belongs to UUID {}. Remove it from authercation-names.json to free it.",
            profileName, profile.id(), NamePinStore.ownerOf(profileName)
        );
        return null;
    }

    @Unique
    private static URL authercation$url(String spec) {
        try {
            return URI.create(spec).toURL();
        } catch (MalformedURLException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid URL: " + spec, e);
        }
    }

    @Unique
    private static String authercation$query(String baseUrl, Map<String, String> arguments) {
        StringBuilder sb = new StringBuilder(baseUrl.contains("?") ? "&" : "?");
        boolean first = true;
        for (Map.Entry<String, String> entry : arguments.entrySet()) {
            if (!first) {
                sb.append('&');
            }
            first = false;
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                .append('=')
                .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    @Unique
    private boolean authercation$existsOnMojang(String profileName) {
        try {
            NameAndId found = client.get(
                authercation$url(
                    MOJANG_LOOKUP_URL + URLEncoder.encode(profileName, StandardCharsets.UTF_8)
                ),
                NameAndId.class
            );
            return found != null;
        } catch (MinecraftClientException e) {
            LOGGER.debug("Mojang name lookup for {} failed: {}", profileName, e.getMessage());
            return false;
        }
    }
}
