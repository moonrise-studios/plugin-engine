package gg.moonrise.engine.paper.toast;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BedrockToastPayloadTest {

    @Test
    public void usesTheStableToastRequestPacketId() {
        assertEquals(186, BedrockToastPayload.TOAST_REQUEST_PACKET_ID);
    }

    @Test
    public void encodesLengthPrefixedStringsBackToBack() {
        byte[] encoded = BedrockToastPayload.encode("Hi", "There");

        assertArrayEquals(
                new byte[]{0x02, 0x48, 0x69, 0x05, 0x54, 0x68, 0x65, 0x72, 0x65},
                encoded
        );
    }

    @Test
    public void encodesEmptyContentAsZeroLength() {
        byte[] encoded = BedrockToastPayload.encode("Hi", "");

        assertArrayEquals(new byte[]{0x02, 0x48, 0x69, 0x00}, encoded);
    }

    @Test
    public void countsBytesNotCharactersForAccentedText() {
        byte[] encoded = BedrockToastPayload.encode("é", "");

        // U+00E9 is two UTF-8 bytes.
        assertArrayEquals(new byte[]{0x02, (byte) 0xC3, (byte) 0xA9, 0x00}, encoded);
    }

    @Test
    public void countsBytesNotCharactersForSurrogatePairs() {
        String emoji = "🔔";
        byte[] emojiBytes = emoji.getBytes(StandardCharsets.UTF_8);

        byte[] encoded = BedrockToastPayload.encode(emoji, "");

        assertEquals(4, emojiBytes.length);
        assertEquals(2, emoji.length());
        assertEquals(4, encoded[0]);
        assertEquals(emojiBytes.length + 2, encoded.length);
    }

    @Test
    public void usesTwoByteVarIntBeyondOneHundredTwentySeven() {
        String title = "a".repeat(130);

        byte[] encoded = BedrockToastPayload.encode(title, "");

        // 130 -> 0x82 0x01 as an unsigned LEB128 VarInt.
        assertEquals((byte) 0x82, encoded[0]);
        assertEquals((byte) 0x01, encoded[1]);
        assertEquals('a', encoded[2]);
        assertEquals(130 + 2 + 1, encoded.length);
    }

    @Test
    public void usesSingleByteVarIntAtTheBoundary() {
        byte[] encoded = BedrockToastPayload.encode("a".repeat(127), "");

        assertEquals((byte) 0x7F, encoded[0]);
        assertEquals(127 + 1 + 1, encoded.length);
    }

    @Test
    public void rejectsNullText() {
        assertThrows(NullPointerException.class, () -> BedrockToastPayload.encode(null, ""));
        assertThrows(NullPointerException.class, () -> BedrockToastPayload.encode("Hi", null));
    }
}
