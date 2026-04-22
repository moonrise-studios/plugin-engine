package gg.moonrise.engine.util;

import gg.moonrise.engine.util.NumberUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NumberUtilTest {

    @Test
    void fancyAddsExpectedSuffixes() {
        assertEquals("1st", NumberUtil.fancy(1));
        assertEquals("12th", NumberUtil.fancy(12));
        assertEquals("23rd", NumberUtil.fancy(23));
    }

    @Test
    void condenseFormatsValues() {
        assertEquals("1.5k", NumberUtil.condense(1500));
        assertEquals("999", NumberUtil.condense(new BigDecimal("999")));
    }
}
