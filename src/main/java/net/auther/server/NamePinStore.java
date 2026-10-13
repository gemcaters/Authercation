package net.auther.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * "First to join owns the name."
 *
 * Maps a lower-cased player name to the UUID that first logged in with it.
 * Later logins using the same name but a different UUID (for example an Ely.by
 * account that copied a premium player's name) are refused.
 *
 * Stored at {@code <config dir>/authercation-names.json}. The file is re-read
 * whenever it changes on disk, so an owner can free a name by deleting its
 * line, no restart needed.
 */
public final class NamePinStore {

    private static final Logger LOGGER = LoggerFactory.getLogger("Authercation/Names");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<TreeMap<String, String>>() {}.getType();

    private static final Map<String, String> PINS = new TreeMap<>();
    private static long loadedModified = Long.MIN_VALUE;
    private static boolean initialised = false;

    private NamePinStore() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("authercation-names.json");
    }

    /**
     * @return true if {@code uuid} may use {@code name} (and the claim is now
     *         recorded); false if the name belongs to a different UUID.
     */
    public static synchronized boolean claim(String name, UUID uuid) {
        refresh();

        String key = name.toLowerCase(Locale.ROOT);
        String id = uuid.toString();
        String owner = PINS.get(key);

        if (owner != null && !owner.equalsIgnoreCase(id)) {
            return false;
        }

        boolean changed = false;
        // The account was renamed: free the names it held before.
        changed |= PINS.entrySet().removeIf(e -> e.getValue().equalsIgnoreCase(id) && !e.getKey().equals(key));
        if (owner == null) {
            PINS.put(key, id);
            changed = true;
        }
        if (changed) {
            save();
        }
        return true;
    }

    /** UUID that owns {@code name}, or null. For log messages. */
    public static synchronized String ownerOf(String name) {
        refresh();
        return PINS.get(name.toLowerCase(Locale.ROOT));
    }

    private static void refresh() {
        Path path = file();
        try {
            if (!initialised) {
                initialised = true;
                if (Files.exists(path)) {
                    load(path);
                } else {
                    seedFromUserCache();
                    save();
                }
                return;
            }
            if (Files.exists(path) && Files.getLastModifiedTime(path).toMillis() != loadedModified) {
                load(path);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.error("Could not refresh name pins, keeping the in-memory copy", e);
        }
    }

    private static void load(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path)) {
            Map<String, String> read = GSON.fromJson(reader, MAP_TYPE);
            PINS.clear();
            if (read != null) {
                read.forEach((k, v) -> PINS.put(k.toLowerCase(Locale.ROOT), v));
            }
        }
        loadedModified = Files.getLastModifiedTime(path).toMillis();
    }

    private static void save() {
        Path path = file();
        Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.getParent());
            try (var writer = Files.newBufferedWriter(tmp)) {
                GSON.toJson(PINS, MAP_TYPE, writer);
            }
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            loadedModified = Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            LOGGER.error("Could not save name pins", e);
        }
    }

    /**
     * First run only: treat everyone already in {@code usercache.json} as the
     * rightful owner of their name, so a newcomer cannot squat the name of a
     * returning player. Offline-mode UUIDs (version 3) are skipped, because
     * they would lock real accounts out of their own names.
     */
    private static void seedFromUserCache() {
        Path cache = FabricLoader.getInstance().getGameDir().resolve("usercache.json");
        if (!Files.exists(cache)) {
            return;
        }
        int seeded = 0;
        try (var reader = Files.newBufferedReader(cache)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonArray()) {
                return;
            }
            JsonArray entries = root.getAsJsonArray();
            for (JsonElement element : entries) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject entry = element.getAsJsonObject();
                if (!entry.has("name") || !entry.has("uuid")) {
                    continue;
                }
                try {
                    UUID uuid = UUID.fromString(entry.get("uuid").getAsString());
                    if (uuid.version() == 3) {
                        continue;
                    }
                    PINS.putIfAbsent(entry.get("name").getAsString().toLowerCase(Locale.ROOT), uuid.toString());
                    seeded++;
                } catch (IllegalArgumentException ignored) {
                    // malformed uuid, skip
                }
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read usercache.json for seeding: {}", e.getMessage());
        }
        LOGGER.info("Seeded {} name(s) from usercache.json", seeded);
    }
}
