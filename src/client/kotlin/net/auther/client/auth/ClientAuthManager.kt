package net.auther.client.auth

import net.auther.Authercation
import org.slf4j.LoggerFactory

/**
 * Client-side authentication state manager.
 *
 * Tracks which provider was used to log in on the client so that the
 * correct session token can be set before connecting to a server.
 *
 * Tokens are **never** stored to disk or logged.
 */
object ClientAuthManager {

    private val logger = LoggerFactory.getLogger("AltAuth/Client")

    enum class Provider { MICROSOFT, ELY_BY, NONE }

    /** The provider that authenticated the current session. */
    var activeProvider: Provider = Provider.NONE
        private set

    /**
     * Called when the user has successfully authenticated via Microsoft.
     * The Minecraft launcher already handles token management for this path;
     * we just track provider state.
     */
    fun onMicrosoftLogin() {
        activeProvider = Provider.MICROSOFT
        logger.info("Active provider set to Microsoft")
    }

    /**
     * Called when the user has successfully authenticated via Ely.by.
     * The auth-lib injector handles the actual token; we track provider state.
     */
    fun onElyByLogin() {
        activeProvider = Provider.ELY_BY
        logger.info("Active provider set to Ely.by")
    }

    /** Reset auth state (e.g. on logout). */
    fun reset() {
        activeProvider = Provider.NONE
    }
}
