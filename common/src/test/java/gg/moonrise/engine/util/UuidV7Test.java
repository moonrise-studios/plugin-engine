package gg.moonrise.engine.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UuidV7Test {

    @Test
    void generatesVersionSevenUuidWithRfcVariant() {
        UUID uuid = UuidV7.generate();

        assertEquals(7, uuid.version());
        assertEquals(2, uuid.variant());
    }

    @Test
    void preservesTimestampMillis() {
        long timestamp = 1_764_502_800_123L;
        UUID uuid = UuidV7.generateAt(timestamp);

        assertEquals(timestamp, UuidV7.timestampMillis(uuid));
    }

    @Test
    void roundTripsBytes() {
        UUID uuid = UuidV7.generateAt(1_764_502_800_123L);

        assertEquals(uuid, UuidV7.fromBytes(UuidV7.toBytes(uuid)));
    }

    @Test
    void ordersByTimestamp() {
        UUID older = UuidV7.generateAt(1_764_502_800_123L);
        UUID newer = UuidV7.generateAt(1_764_502_800_124L);

        assertTrue(older.compareTo(newer) < 0);
    }

    @Test
    void rejectsInvalidBytes() {
        assertThrows(IllegalArgumentException.class, () -> UuidV7.fromBytes(null));
        assertThrows(IllegalArgumentException.class, () -> UuidV7.fromBytes(new byte[15]));
        assertThrows(IllegalArgumentException.class, () -> UuidV7.fromBytes(new byte[17]));
    }
}
