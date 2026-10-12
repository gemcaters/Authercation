package net.auther.server;

import net.auther.auth.AuthProvider;
import net.auther.auth.elyby.ElyByAuthProvider;
import net.auther.config.AltAuthConfig;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Holds the active configuration and the list of fallback [AuthProvider]s.
 *
 * Mojang/Microsoft accounts are verified by the vanilla login flow (the server
 * must stay on `online-mode=true`). The providers here are only asked when
 * Mojang rejects a session. See `HasJoinedServerMixin`.
 */
public final class AuthSessionHandler {

    public static final AuthSessionHandler INSTANCE = new AuthSessionHandler();

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("Authercation/SessionHandler");

    private static final Map<String, AuthProvider> knownProviders = new HashMap<>();
    static {
        knownProviders.put("elyby", new ElyByAuthProvider());
    }

    /** Ids that mean "vanilla Mojang check"; always active, nothing to load. */
    private static final Set<String> vanillaIds = Set.of("microsoft", "mojang");

    private volatile AltAuthConfig config = new AltAuthConfig();
    private volatile List<AuthProvider> fallbackProviders = Collections.emptyList();

    private AuthSessionHandler() {}

    public AltAuthConfig getConfig() {
        return config;
    }

    public List<AuthProvider> getFallbackProviders() {
        return fallbackProviders;
    }

    public void init(AltAuthConfig config) {
        this.config = config;

        List<AuthProvider> providers = new ArrayList<>();
        for (String id : config.getEnabledProviders()) {
            String key = id.toLowerCase(Locale.ROOT);
            if (vanillaIds.contains(key)) {
                continue;
            }
            AuthProvider provider = knownProviders.get(key);
            if (provider == null) {
                logger.warn("Unknown provider '{}' in config, skipping", id);
            } else {
                providers.add(provider);
            }
        }

        this.fallbackProviders = providers;

        String joined = "";
        for (int i = 0; i < providers.size(); i++) {
            if (i > 0) joined += ", ";
            joined += providers.get(i).getId();
        }

        logger.info(
            "Mojang/Microsoft handled by vanilla. Fallback providers ({}): {}",
            providers.size(),
            joined
        );
    }
}
