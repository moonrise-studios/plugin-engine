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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChestMenuTest extends MockBukkitTest {

    @Test
    void addRemoveAndSlotBoundsWork() {
        PlayerMock player = server.addPlayer();
        TestChestMenu menu = new TestChestMenu(player, 1);
        Button button = button(Material.DIAMOND);

        menu.addButton(0, button);

        assertTrue(menu.hasButton(0));
        assertEquals(1, menu.getButtonCount());
        assertEquals(button, menu.removeButton(0));
        assertFalse(menu.hasButton(0));
        assertNull(button.boundingInventory());
        assertThrows(IllegalArgumentException.class, () -> menu.addButton(9, button));
    }

    @Test
    void refreshWritesButtonItemsAndPersistentIds() {
        PlayerMock player = server.addPlayer();
        TestChestMenu menu = new TestChestMenu(player, 1);
        Button button = Button.builder()
                .item(viewer -> new ItemStack(Material.EMERALD))
                .refresh(10L)
                .build();

        menu.addButton(4, button);
        menu.refresh();

        Inventory inventory = menu.getInventory();
        assertNotNull(inventory);

        ItemStack rendered = inventory.getItem(4);
        assertNotNull(rendered);
        assertEquals(Material.EMERALD, rendered.getType());
        ItemMeta meta = rendered.getItemMeta();
        assertNotNull(meta);
        assertEquals(button.uuid().toString(), meta.getPersistentDataContainer().get(Button.KEY, PersistentDataType.STRING));
        assertEquals(inventory, button.inventory());
        assertEquals(1, menu.getRefreshingButtons().size());
        assertTrue(menu.checkCancelClick(4));
    }

    private static Button button(Material material) {
        return Button.builder()
                .item(player -> new ItemStack(material))
                .build();
    }

    private static final class TestChestMenu extends ChestMenu {

        private TestChestMenu(Player player, int rows) {
            super(player, "Test", rows);
        }
    }
}
