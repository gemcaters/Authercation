package net.auther.server

import net.auther.auth.AuthProvider
import net.auther.auth.elyby.ElyByAuthProvider
import net.auther.config.AltAuthConfig
import org.slf4j.LoggerFactory

/**
 * Holds the active configuration and the list of fallback [AuthProvider]s.
 *
 * Mojang/Microsoft accounts are verified by the vanilla login flow (the server
 * must stay on `online-mode=true`). The providers here are only asked when
 * Mojang rejects a session. See `HasJoinedServerMixin`.
 */
object AuthSessionHandler {

    private val logger = LoggerFactory.getLogger("Authercation/SessionHandler")

    private val knownProviders: Map<String, AuthProvider> = mapOf(
        "elyby" to ElyByAuthProvider(),
    )

    /** Ids that mean "vanilla Mojang check"; always active, nothing to load. */
    private val vanillaIds = setOf("microsoft", "mojang")

    @Volatile
    var config: AltAuthConfig = AltAuthConfig()
        private set

    /** Providers tried, in order, after Mojang rejects a session. */
    @Volatile
    var fallbackProviders: List<AuthProvider> = emptyList()
        private set

    fun init(config: AltAuthConfig) {
        this.config = config

        fallbackProviders = config.enabledProviders.mapNotNull { id ->
            val key = id.lowercase()
            when {
                key in vanillaIds -> null
                else -> knownProviders[key].also {
                    if (it == null) logger.warn("Unknown provider '{}' in config, skipping", id)
                }
            }
        }

        logger.info(
            "Mojang/Microsoft handled by vanilla. Fallback providers ({}): {}",
            fallbackProviders.size,
            fallbackProviders.joinToString { it.id }
        )
    }
}
