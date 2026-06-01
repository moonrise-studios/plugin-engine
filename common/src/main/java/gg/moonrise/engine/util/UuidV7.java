package gg.moonrise.engine.util;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.UUID;

/**
 * Utility methods for UUID version 7 values.
 */
public final class UuidV7 {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int UUID_BYTES = 16;

    private UuidV7() {
    }

    /**
     * Generates a UUIDv7 using the current system time.
     *
     * @return generated UUIDv7
     */
    public static UUID generate() {
        return generateAt(System.currentTimeMillis());
    }

    /**
     * Generates a UUIDv7 using the supplied Unix timestamp in milliseconds.
     *
     * @param timestampMillis timestamp to encode in the UUID
     * @return generated UUIDv7
     */
    public static UUID generateAt(long timestampMillis) {
        int randA = RANDOM.nextInt(0x1000);
        long most = (timestampMillis & 0xFFFFFFFFFFFFL) << 16;
        most |= 0x7000L;
        most |= randA;

        long least = 0x8000000000000000L | (RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL);
        return new UUID(most, least);
    }

    /**
     * Reads the Unix timestamp in milliseconds encoded in a UUIDv7.
     *
     * @param uuid UUIDv7 value
     * @return encoded timestamp in milliseconds
     */
    public static long timestampMillis(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");
        return (uuid.getMostSignificantBits() >>> 16) & 0xFFFFFFFFFFFFL;
    }

    /**
     * Converts a UUID to its 16-byte big-endian representation.
     *
     * @param uuid UUID value
     * @return UUID bytes
     */
    public static byte[] toBytes(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid");

        ByteBuffer buffer = ByteBuffer.allocate(UUID_BYTES);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    /**
     * Reads a UUID from its 16-byte big-endian representation.
     *
     * @param bytes UUID bytes
     * @return UUID value
     * @throws IllegalArgumentException if the array is not exactly 16 bytes
     */
    public static UUID fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length != UUID_BYTES) {
            throw new IllegalArgumentException("UUID bytes must be exactly 16 bytes");
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }
}
