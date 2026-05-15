package gg.moonrise.engine.paper.data;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AABBTest {

    @Test
    void convertsToVectorsAndArrays() {
        AABB box = new AABB(1.0, 2.0, 3.0, 4.0, 5.0, 6.0);

        assertEquals(new Vector(1.0, 2.0, 3.0), box.minVector());
        assertEquals(new Vector(4.0, 5.0, 6.0), box.maxVector());
        assertEquals(box.minVector(), box.toVectors().first());
        assertEquals(box.maxVector(), box.toVectors().second());
        assertArrayEquals(new double[]{1.0, 2.0, 3.0}, box.minArray());
        assertArrayEquals(new double[]{4.0, 5.0, 6.0}, box.maxArray());
        assertArrayEquals(new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0}, box.toArrayPair());
    }

    @Test
    void insideCheckIncludesBoundaries() {
        AABB box = new AABB(1.0, 2.0, 3.0, 4.0, 5.0, 6.0);

        assertTrue(box.isInside(1.0, 2.0, 3.0));
        assertTrue(box.isInside(new Vector(4.0, 5.0, 6.0)));
        assertTrue(box.isInside(2.5, 3.5, 4.5));
        assertFalse(box.isInside(0.99, 3.5, 4.5));
        assertFalse(box.isInside(2.5, 5.01, 4.5));
    }

    @Test
    void createsFromVectorsAndArrayPair() {
        AABB fromVectors = AABB.fromVectors(new Vector(1.0, 2.0, 3.0), new Vector(4.0, 5.0, 6.0));
        AABB fromArray = AABB.fromArrayPair(new double[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0});

        assertEquals(fromVectors, fromArray);
        assertThrows(IllegalArgumentException.class, () -> AABB.fromArrayPair(new double[]{1.0, 2.0}));
    }
}
