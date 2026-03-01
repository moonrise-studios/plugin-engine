package games.negative.engine.util;

import games.negative.engine.util.TimeUtil;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeUtilTest {

    @Test
    void formatAndParseRoundTripUnitFormat() {
        Duration duration = TimeUtil.parse("1h30m");
        assertEquals(Duration.ofMinutes(90), duration);
        assertEquals("1h 30m", TimeUtil.format(duration, true));
    }

    @Test
    void formatAndParseRoundTripColonFormat() {
        Duration duration = Duration.ofHours(1).plusMinutes(5).plusSeconds(2);
        String formatted = TimeUtil.formatColonSeparated(duration);
        assertEquals("1:05:02", formatted);
        assertEquals(duration, TimeUtil.parseColonSeparated(formatted));
    }
}
