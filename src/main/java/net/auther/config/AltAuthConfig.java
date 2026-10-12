package net.auther.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.auther.Authercation;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Simple JSON configuration for AltAuth.
 *
 * Located at `<config dir>/altauth.json`.
 *
 * @param enabledProviders  Fallback providers tried (in order) after Mojang rejects a session.
 *                          Supported: "elyby". "microsoft"/"mojang" are accepted but ignored,
 *                          since vanilla always checks Mojang first.
 * @param preventFallbackIfPlayerExists  When true, a name that exists on Mojang can never be
 *                          claimed through a fallback provider (blocks Ely.by users who pick a
 *                          premium player's name). Default false.
 */
public final class AltAuthConfig {
    private final List<String> enabledProviders;
    private final boolean preventFallbackIfPlayerExists;

    public AltAuthConfig(List<String> enabledProviders, boolean preventFallbackIfPlayerExists) {
        this.enabledProviders = enabledProviders != null ? enabledProviders : new ArrayList<>();
        this.preventFallbackIfPlayerExists = preventFallbackIfPlayerExists;
    }

    public AltAuthConfig() {
        this.enabledProviders = new ArrayList<>();
        this.enabledProviders.add("elyby");
        this.preventFallbackIfPlayerExists = false;
    }

    public List<String> getEnabledProviders() {
        return enabledProviders;
    }

    public boolean getPreventFallbackIfPlayerExists() {
        return preventFallbackIfPlayerExists;
    }

    public boolean isPreventFallbackIfPlayerExists() {
        return preventFallbackIfPlayerExists;
    }

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger("AltAuth/Config");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("altauth.json");
    }

    /**
     * Load config from disk, writing defaults if the file does not exist.
     */
    public static AltAuthConfig load() {
        Path path = configPath();
        if (Files.exists(path)) {
            try (java.io.BufferedReader reader = Files.newBufferedReader(path)) {
                return gson.fromJson(reader, AltAuthConfig.class);
            } catch (Exception e) {
                logger.error("Failed to read altauth.json, using defaults", e);
                return new AltAuthConfig();
            }
        } else {
            AltAuthConfig defaults = new AltAuthConfig();
            defaults.save();
            return defaults;
        }
    }

    /**
     * Save this config instance to disk.
     */
    public void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            try (java.io.BufferedWriter writer = Files.newBufferedWriter(path)) {
                gson.toJson(this, writer);
            }
            logger.info("Wrote default altauth.json to {}", path);
        } catch (IOException e) {
            logger.error("Failed to write altauth.json", e);
        }
    }
}
