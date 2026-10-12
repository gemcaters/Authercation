package net.auther;

import net.auther.config.AltAuthConfig;
import net.auther.server.AuthSessionHandler;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Authercation implements ModInitializer {

    public static final String MOD_ID = "authercation";

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Active config, loaded during [onInitialize]. */
    private static AltAuthConfig config;

    @Override
    public void onInitialize() {
        LOGGER.info("AltAuth initializing...");

        // Load (or create default) config
        config = AltAuthConfig.load();

        // Boot the server-side session handler with the loaded config
        AuthSessionHandler.INSTANCE.init(config);

        LOGGER.info("AltAuth ready. Providers: {}", config.getEnabledProviders());
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static AltAuthConfig getConfig() {
        return config;
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    // Documented mitigation for "Invalid signature for profile public key"
    // when using third-party auth providers such as Ely.by.
    //
    // When a non-Mojang provider authenticates a session, the player's
    // profile properties are not signed by Mojang, so the server's
    // enforce-secure-profile check can reject the join. Installing
    // No Chat Reports on the server suppresses that check.
    //
    // Reference: https://docs.ely.by/en/minecraft-auth.html
    public static final boolean NO_CHAT_REPORTS_HELPS_WITH_THIRD_PARTY_AUTH = true;
}
