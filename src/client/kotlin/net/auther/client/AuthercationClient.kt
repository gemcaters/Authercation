package net.auther.client

import net.auther.client.auth.ClientAuthManager
import net.fabricmc.api.ClientModInitializer
import org.slf4j.LoggerFactory

object AuthercationClient : ClientModInitializer {

    private val logger = LoggerFactory.getLogger("AltAuth/Client")

    override fun onInitializeClient() {
        logger.info("AltAuth client initializing...")

        // Default to Microsoft since that is what the vanilla launcher provides
        ClientAuthManager.onMicrosoftLogin()

        logger.info("AltAuth client ready. Active provider: {}", ClientAuthManager.activeProvider)
    }
}