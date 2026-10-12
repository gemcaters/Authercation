package net.auther.client;

import net.auther.client.auth.ClientAuthManager;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AuthercationClient implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("AltAuth/Client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("AltAuth client initializing...");

        // Default to Microsoft since that is what the vanilla launcher provides
        ClientAuthManager.onMicrosoftLogin();

        LOGGER.info("AltAuth client ready. Active provider: {}", ClientAuthManager.getActiveProvider());
    }
}
