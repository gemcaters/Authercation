package net.auther.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import net.auther.Authercation
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

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
data class AltAuthConfig(
    val enabledProviders: List<String> = listOf("elyby"),
    val preventFallbackIfPlayerExists: Boolean = false,
) {
    companion object {
        private val logger = LoggerFactory.getLogger("AltAuth/Config")
        private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

        private val configPath: Path
            get() = FabricLoader.getInstance().configDir.resolve("altauth.json")

        /** Load config from disk, writing defaults if the file does not exist. */
        fun load(): AltAuthConfig {
            val path = configPath
            return if (Files.exists(path)) {
                try {
                    Files.newBufferedReader(path).use { reader ->
                        gson.fromJson(reader, AltAuthConfig::class.java)
                    }
                } catch (e: Exception) {
                    logger.error("Failed to read altauth.json, using defaults", e)
                    AltAuthConfig()
                }
            } else {
                val defaults = AltAuthConfig()
                defaults.save()
                defaults
            }
        }

        /** Save this config instance to disk. */
        fun AltAuthConfig.save() {
            val path = configPath
            try {
                Files.createDirectories(path.parent)
                Files.newBufferedWriter(path).use { writer ->
                    gson.toJson(this, writer)
                }
                logger.info("Wrote default altauth.json to {}", path)
            } catch (e: IOException) {
                logger.error("Failed to write altauth.json", e)
            }
        }
    }
}
