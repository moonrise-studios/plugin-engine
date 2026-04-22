package gg.moonrise.engine.util;

import gg.moonrise.engine.util.IntList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntListTest {

    @Test
    void parseSupportsSinglesAndRanges() {
        List<Integer> values = IntList.parse(List.of("1", "3-5", " 7 "));

        assertEquals(List.of(1, 3, 4, 5, 7), values);
    }
}
