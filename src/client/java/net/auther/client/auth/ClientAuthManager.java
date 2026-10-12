package net.auther.client.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-side authentication state manager.
 *
 * Tracks which provider was used to log in on the client so that the
 * correct session token can be set before connecting to a server.
 *
 * Tokens are <b>never</b> stored to disk or logged.
 */
public final class ClientAuthManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("AltAuth/Client");

    public enum Provider { MICROSOFT, ELY_BY, NONE }

    private static volatile Provider activeProvider = Provider.NONE;

    private ClientAuthManager() {}

    /** The provider that authenticated the current session. */
    public static Provider getActiveProvider() {
        return activeProvider;
    }

    /**
     * Called when the user has successfully authenticated via Microsoft.
     * The Minecraft launcher already handles token management for this path;
     * we just track provider state.
     */
    public static void onMicrosoftLogin() {
        activeProvider = Provider.MICROSOFT;
        LOGGER.info("Active provider set to Microsoft");
    }

    /**
     * Called when the user has successfully authenticated via Ely.by.
     * The authlib-injector handles the actual token; we track provider state.
     */
    public static void onElyByLogin() {
        activeProvider = Provider.ELY_BY;
        LOGGER.info("Active provider set to Ely.by");
    }

    /** Reset auth state (e.g. on logout). */
    public static void reset() {
        activeProvider = Provider.NONE;
    }
}
