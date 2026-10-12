package net.auther

import net.auther.config.AltAuthConfig
import net.auther.config.AltAuthConfig.Companion.save
import net.auther.server.AuthSessionHandler
import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object Authercation : ModInitializer {

    const val MOD_ID: String = "authercation"

    val LOGGER = LoggerFactory.getLogger(MOD_ID)

    /** Active config, loaded during [onInitialize]. */
    lateinit var config: AltAuthConfig
        private set

    override fun onInitialize() {
        LOGGER.info("AltAuth initializing...")

        // Load (or create default) config
        config = AltAuthConfig.load()

        // Boot the server-side session handler with the loaded config
        AuthSessionHandler.init(config)

        LOGGER.info("AltAuth ready. Providers: {}", config.enabledProviders)
    }

    fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)

    companion object {
        // Documented mitigation for "Invalid signature for profile public key"
        // when using third-party auth providers such as Ely.by.
        //
        // When a non-Mojang provider authenticates a session, the player's
        // profile properties are not signed by Mojang, so the server's
        // enforce-secure-profile check can reject the join. Installing
        // No Chat Reports on the server suppresses that check.
        //
        // Reference: https://docs.ely.by/en/minecraft-auth.html
        const val NO_CHAT_REPORTS_HELPS_WITH_THIRD_PARTY_AUTH: Boolean = true
    }
}
