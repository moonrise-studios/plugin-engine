package gg.moonrise.engine.paper.gui;

import gg.moonrise.engine.paper.gui.button.Button;
import gg.moonrise.engine.paper.support.MockBukkitTest;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PaginatedMenuTest extends MockBukkitTest {

    @Test
    void hiddenNextPageControlLeavesFixedButtonVisible() {
        PlayerMock player = server.addPlayer();
        TestPaginatedMenu menu = new TestPaginatedMenu(player, 1, List.of(0, 1, 2));
        Button fallback = button(Material.IRON_INGOT);

        menu.addButton(2, fallback);
        menu.nextPageButton(2, button(Material.GOLD_INGOT));
        menu.setContent(List.of(button(Material.DIAMOND)));

        menu.refresh();

        assertItem(menu.getInventory(), 0, Material.DIAMOND);
        assertItem(menu.getInventory(), 2, Material.IRON_INGOT);
    }

    @Test
    void hiddenPreviousPageControlLeavesFixedButtonVisible() {
        PlayerMock player = server.addPlayer();
        TestPaginatedMenu menu = new TestPaginatedMenu(player, 1, List.of(0, 1, 2));
        Button fallback = button(Material.IRON_INGOT);

        menu.addButton(0, fallback);
        menu.previousPageButton(0, button(Material.GOLD_INGOT));
        menu.setContent(List.of(button(Material.DIAMOND)));

        menu.refresh();

        assertItem(menu.getInventory(), 0, Material.IRON_INGOT);
        assertItem(menu.getInventory(), 1, Material.DIAMOND);
    }

    @Test
    void visiblePageControlsOverrideFixedButtonsAndReserveContentSlots() {
        PlayerMock player = server.addPlayer();
        TestPaginatedMenu menu = new TestPaginatedMenu(player, 1, List.of(0, 1, 2, 3, 4));

        menu.addButton(0, button(Material.IRON_INGOT));
        menu.addButton(4, button(Material.IRON_INGOT));
        menu.previousPageButton(0, button(Material.REDSTONE));
        menu.nextPageButton(4, button(Material.EMERALD));
        menu.setContent(buttons(Material.DIAMOND, 11));

        menu.changePage(2);

        Inventory inventory = menu.getInventory();
        assertItem(inventory, 0, Material.REDSTONE);
        assertItem(inventory, 1, Material.DIAMOND);
        assertItem(inventory, 2, Material.DIAMOND);
        assertItem(inventory, 3, Material.DIAMOND);
        assertItem(inventory, 4, Material.EMERALD);
    }

    @Test
    void fullLastPageDoesNotRenderNextPageControl() {
        PlayerMock player = server.addPlayer();
        TestPaginatedMenu menu = new TestPaginatedMenu(player, 1, List.of(0, 1));

        menu.nextPageButton(1, button(Material.EMERALD));
        menu.setContent(buttons(Material.DIAMOND, 2));

        menu.refresh();

        assertItem(menu.getInventory(), 0, Material.DIAMOND);
        assertItem(menu.getInventory(), 1, Material.DIAMOND);
    }

    private static List<Button> buttons(Material material, int count) {
        List<Button> buttons = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            buttons.add(button(material));
        }
        return buttons;
    }

    private static Button button(Material material) {
        return Button.builder()
                .item(player -> new ItemStack(material))
                .build();
    }

    private static void assertItem(Inventory inventory, int slot, Material material) {
        assertNotNull(inventory);
        ItemStack item = inventory.getItem(slot);
        assertNotNull(item);
        assertEquals(material, item.getType());
    }

    private static final class TestPaginatedMenu extends PaginatedMenu {

        private TestPaginatedMenu(Player player, int rows, List<Integer> contentSlots) {
            super(player, "Test", rows);
            setContentSlots(contentSlots);
        }

        private void nextPageButton(int slot, Button button) {
            setNextPageButton(slot, button);
        }

        private void previousPageButton(int slot, Button button) {
            setPreviousPageButton(slot, button);
        }
    }
}
