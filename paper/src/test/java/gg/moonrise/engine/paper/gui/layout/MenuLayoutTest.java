package gg.moonrise.engine.paper.gui.layout;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuLayoutTest {

    @Test
    void chestLayoutIgnoresWhitespaceAndIndexesSlots() {
        MenuLayout layout = MenuLayout.chest(
                "# # # # # # # # #",
                "# . . . . . . . #"
        );

        assertEquals(9, layout.width());
        assertEquals(2, layout.height());
        assertEquals(List.of(10, 11, 12, 13, 14, 15, 16), layout.slots('.'));
        assertEquals(0, layout.firstSlot('#'));
        assertTrue(layout.has('.'));
    }

    @Test
    void verticalOrderFillsColumnsBeforeRows() {
        MenuLayout layout = MenuLayout.chest(
                "x.x......",
                "x.x......"
        );

        assertEquals(List.of(0, 2, 9, 11), layout.slots('x'));
        assertEquals(List.of(0, 9, 2, 11), layout.slots('x', ContentSlotOrder.VERTICAL));
    }

    @Test
    void invalidRowsAndMissingKeysFailClearly() {
        assertThrows(IllegalArgumentException.class, () -> MenuLayout.chest("short"));
        assertThrows(IllegalArgumentException.class, () -> MenuLayout.chest(
                ".........",
                ".........",
                ".........",
                ".........",
                ".........",
                ".........",
                "........."
        ));

        MenuLayout layout = MenuLayout.hopper("abcde");
        assertThrows(IllegalArgumentException.class, () -> layout.firstSlot('z'));
    }
}
