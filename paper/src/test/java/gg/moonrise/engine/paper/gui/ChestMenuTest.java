package gg.moonrise.engine.paper.gui;

import gg.moonrise.engine.paper.gui.button.Button;
import gg.moonrise.engine.paper.gui.layout.MenuLayout;
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
import java.util.concurrent.atomic.AtomicBoolean;

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

    @Test
    void layoutFillAddsGeneratedButtons() {
        PlayerMock player = server.addPlayer();
        TestChestMenu menu = new TestChestMenu(player, 1);
        MenuLayout layout = MenuLayout.chest("#########");

        menu.addButtons(layout, '#', slot -> button(slot == 8 ? Material.GOLD_INGOT : Material.GRAY_STAINED_GLASS_PANE));
        menu.refresh();

        assertEquals(9, menu.getButtonCount());
        assertEquals(Material.GRAY_STAINED_GLASS_PANE, menu.getInventory().getItem(0).getType());
        assertEquals(Material.GOLD_INGOT, menu.getInventory().getItem(8).getType());
    }

    @Test
    void replacingButtonDetachesDisplacedButtonAndRefreshTracking() {
        PlayerMock player = server.addPlayer();
        TestChestMenu menu = new TestChestMenu(player, 1);
        Button filler = Button.builder()
                .item(viewer -> new ItemStack(Material.GRAY_STAINED_GLASS_PANE))
                .refresh(20L)
                .build();
        Button control = button(Material.EMERALD);

        menu.addButton(4, filler);
        menu.refresh();
        assertEquals(menu.getInventory(), filler.inventory());
        assertEquals(1, menu.getRefreshingButtons().size());

        menu.addButton(4, control);
        menu.refresh();

        assertNull(filler.inventory());
        assertEquals(-1, filler.slot());
        assertEquals(0, menu.getRefreshingButtons().size());
        assertEquals(Material.EMERALD, menu.getInventory().getItem(4).getType());
    }

    @Test
    void invalidButtonUuidDoesNotThrowWhenCheckingCancelState() {
        PlayerMock player = server.addPlayer();
        TestChestMenu menu = new TestChestMenu(player, 1);
        Button button = button(Material.DIAMOND);

        menu.addButton(0, button);
        menu.refresh();

        ItemStack rendered = menu.getInventory().getItem(0);
        assertNotNull(rendered);
        ItemMeta meta = rendered.getItemMeta();
        assertNotNull(meta);
        meta.getPersistentDataContainer().set(Button.KEY, PersistentDataType.STRING, "not-a-uuid");
        rendered.setItemMeta(meta);

        assertFalse(menu.checkCancelClick(0));
    }

    @Test
    void refreshingButtonClearsStaleItemWhenProviderReturnsAir() {
        PlayerMock player = server.addPlayer();
        TestChestMenu menu = new TestChestMenu(player, 1);
        AtomicBoolean visible = new AtomicBoolean(true);
        Button button = Button.builder()
                .item(viewer -> visible.get() ? new ItemStack(Material.DIAMOND) : new ItemStack(Material.AIR))
                .build();

        menu.addButton(0, button);
        menu.refresh();
        assertEquals(Material.DIAMOND, menu.getInventory().getItem(0).getType());

        visible.set(false);
        menu.refreshButton(0, button);

        assertNull(menu.getInventory().getItem(0));
        assertNull(button.inventory());
        assertEquals(-1, button.slot());
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
