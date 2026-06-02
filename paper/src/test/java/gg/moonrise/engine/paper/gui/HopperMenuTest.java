package gg.moonrise.engine.paper.gui;

import gg.moonrise.engine.paper.gui.button.Button;
import gg.moonrise.engine.paper.support.MockBukkitTest;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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

        Inventory inventory = menu.getInventory();
        assertNotNull(inventory);

        ItemStack rendered = inventory.getItem(2);
        assertNotNull(rendered);
        assertEquals(Material.HOPPER, rendered.getType());
        ItemMeta meta = rendered.getItemMeta();
        assertNotNull(meta);
        assertEquals(button.uuid().toString(), meta.getPersistentDataContainer().get(Button.KEY, PersistentDataType.STRING));
        assertEquals(inventory, button.inventory());
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
