package net.auther.auth;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Gives accounts from a fallback provider their own UUID namespace.
 *
 * The provider's real UUID is hashed together with the provider id and stored
 * as a version-8 (custom) UUID. Properties:
 * <ul>
 *   <li>Stable: the same account always maps to the same UUID, even if it renames.</li>
 *   <li>Cannot collide with Mojang UUIDs (version 4) or offline UUIDs (version 3).</li>
 *   <li>Recognisable: {@link #isForeign(UUID)} is true for every mapped UUID.</li>
 * </ul>
 */
public final class ProviderUuids {

    private static final int FOREIGN_VERSION = 8;

    private ProviderUuids() {}

    public static UUID map(String providerId, UUID original) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hash = sha.digest((providerId + ":" + original).getBytes(StandardCharsets.UTF_8));
            hash[6] = (byte) ((hash[6] & 0x0f) | (FOREIGN_VERSION << 4));
            hash[8] = (byte) ((hash[8] & 0x3f) | 0x80);
            ByteBuffer buffer = ByteBuffer.wrap(hash);
            return new UUID(buffer.getLong(), buffer.getLong());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }

    /** True for UUIDs produced by {@link #map}, i.e. accounts from a fallback provider. */
    public static boolean isForeign(UUID uuid) {
        return uuid.version() == FOREIGN_VERSION;
    }
}
