package games.negative.engine.util;

import games.negative.engine.util.OptionalBool;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptionalBoolTest {

    @Test
    void ofReturnsSingletonInstances() {
        assertSame(OptionalBool.of(true), OptionalBool.of(true));
        assertSame(OptionalBool.of(false), OptionalBool.of(false));
    }

    @Test
    void mapIfTrueAndFalseBehaveAsExpected() {
        Optional<String> whenTrue = OptionalBool.of(true).mapIfTrue(() -> "value");
        Optional<String> whenFalse = OptionalBool.of(false).mapIfTrue(() -> "value");

        assertEquals(Optional.of("value"), whenTrue);
        assertTrue(whenFalse.isEmpty());
    }

    @Test
    void ifTrueOrElseRunsCorrectBranch() {
        AtomicInteger marker = new AtomicInteger(0);
        OptionalBool.of(false).ifTrueOrElse(
                () -> marker.set(1),
                () -> marker.set(2)
        );

        assertFalse(OptionalBool.of(false).isTrue());
        assertEquals(2, marker.get());
    }
}
