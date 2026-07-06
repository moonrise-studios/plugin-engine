package gg.moonrise.engine.paper.gui.button;

import gg.moonrise.engine.paper.support.MockBukkitTest;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ButtonTest extends MockBukkitTest {

    @Test
    void builderRequiresDisplayItem() {
        assertThrows(IllegalStateException.class, () -> Button.builder().build());
    }

    @Test
    void builderStoresDefaultsAndRefreshInterval() {
        Button button = Button.builder()
                .item(player -> new ItemStack(Material.DIAMOND))
                .refresh(20L)
                .build();

        assertNotNull(button.uuid());
        assertTrue(button.cancelClick());
        assertEquals(20L, button.refreshIntervalTicks());
        button.setLastRefreshTime(40L);
        assertEquals(40L, button.lastRefreshTime());
    }

    @Test
    void customCancelAndClickActionAreApplied() {
        AtomicBoolean clicked = new AtomicBoolean(false);
        AtomicReference<Button> clickedButton = new AtomicReference<>();
        Button button = Button.builder()
                .item(player -> new ItemStack(Material.STONE))
                .cancelClick(false)
                .action((currentButton, player, event) -> {
                    clickedButton.set(currentButton);
                    clicked.set(true);
                })
                .build();

        assertFalse(button.cancelClick());
        button.processClickAction(null, null);
        assertTrue(clicked.get());
        assertSame(button, clickedButton.get());
    }

    @Test
    void fixedItemFactoryClonesCallerOwnedStack() {
        ItemStack source = new ItemStack(Material.DIAMOND);
        Button button = Button.of(source);

        ItemStack rendered = button.item(null);
        assertNotNull(rendered);
        assertEquals(Material.DIAMOND, rendered.getType());
        assertNotSame(source, rendered);
    }
}
