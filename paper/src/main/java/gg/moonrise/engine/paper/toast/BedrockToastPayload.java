package gg.moonrise.engine.paper.toast;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Encodes the body of a Bedrock {@code ToastRequestPacket}.
 * <p>
 * Internal; not part of the supported API. It is kept free of Floodgate imports so the wire
 * format stays unit testable on servers that do not have Floodgate installed.
 * <p>
 * The body is two Bedrock strings back to back, each written as an unsigned LEB128 VarInt
 * byte length followed by the UTF-8 bytes. The packet id itself is not part of the body:
 * Floodgate prepends it.
 */
public final class BedrockToastPayload {

    /**
     * The Bedrock {@code ToastRequestPacket} id, stable from protocol v527 through the
     * current codec.
     * <p>
     * Floodgate writes this id as a single byte, so only ids up to {@code 255} can be
     * expressed through that channel. {@code 186} (0xBA) fits comfortably.
     */
    public static final int TOAST_REQUEST_PACKET_ID = 186;

    private static final int VAR_INT_CONTINUATION_BIT = 0x80;
    private static final int VAR_INT_SEGMENT_MASK = 0x7F;

    private BedrockToastPayload() {
    }

    /**
     * Encodes a toast request body.
     * @param title the toast title, in legacy section formatting
     * @param content the toast content, in legacy section formatting
     * @return the encoded packet body, without the packet id
     */
    public static byte[] encode(String title, String content) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(content, "content");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeString(out, title);
        writeString(out, content);
        return out.toByteArray();
    }

    private static void writeString(ByteArrayOutputStream out, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        // Bedrock string lengths count bytes, not characters.
        writeUnsignedVarInt(out, bytes.length);
        out.write(bytes, 0, bytes.length);
    }

    private static void writeUnsignedVarInt(ByteArrayOutputStream out, int value) {
        int remaining = value;

        while ((remaining & ~VAR_INT_SEGMENT_MASK) != 0) {
            out.write((remaining & VAR_INT_SEGMENT_MASK) | VAR_INT_CONTINUATION_BIT);
            remaining >>>= 7;
        }

        out.write(remaining);
    }
}
