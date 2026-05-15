package gg.moonrise.engine.paper.gui;

import gg.moonrise.engine.paper.gui.button.Button;
import gg.moonrise.engine.paper.support.MockBukkitTest;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HopperMenuTest extends MockBukkitTest {

    @Test
    void refreshWritesButtonItemsAndPersistentIds() {
        PlayerMock player = server.addPlayer();
        TestHopperMenu menu = new TestHopperMenu(player);
        Button button = Button.builder()
                .item(viewer -> new ItemStack(Material.HOPPER))
                .refresh(5L)
                .build();

        menu.addButton(2, button);
        menu.refresh();

        ItemStack rendered = menu.getView().getTopInventory().getItem(2);
        assertNotNull(rendered);
        assertEquals(Material.HOPPER, rendered.getType());
        assertEquals(button.uuid().toString(), rendered.getPersistentDataContainer().get(Button.KEY, PersistentDataType.STRING));
        assertEquals(menu.getView(), button.boundingInventory());
        assertEquals(1, menu.getRefreshingButtons().size());
        assertTrue(menu.checkCancelClick(2));
    }

    @Test
    void cancelClicksCanBeDisabled() {
        TestHopperMenu menu = new TestHopperMenu(server.addPlayer());

        assertTrue(menu.cancelClicks());
        menu.cancelClicks(false);
        assertFalse(menu.cancelClicks());
    }

    private static final class TestHopperMenu extends HopperMenu {

        private TestHopperMenu(Player player) {
            super(player, "Test");
        }
    }
}
