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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScrollingMenuTest extends MockBukkitTest {

    private static final List<String> HORIZONTAL_SHAPE = List.of(
            "x x x x x x x x x x x x x x x x x x x",
            "x x x x x x x x x x x x x x x x x x x",
            "x x x x x x x x x x x x x x x x x x x"
    );

    private static final List<String> VERTICAL_SHAPE = List.of(
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x",
            "x x x x x x x x x"
    );

    @Test
    void verticalScrollingRemainsTheDefault() {
        PlayerMock player = server.addPlayer();
        TestScrollingMenu menu = new TestScrollingMenu(player, 2, List.of(0, 1, 2, 9, 10, 11));

        menu.nextLineButton(17, button(Material.EMERALD));
        menu.setContent(numberedButtons(8));
        menu.refresh();

        assertEquals(ScrollDirection.VERTICAL, menu.getScrollDirection());
        assertItemAmount(menu.getInventory(), 0, 1);
        assertItemAmount(menu.getInventory(), 1, 2);
        assertItemAmount(menu.getInventory(), 2, 3);
        assertItemAmount(menu.getInventory(), 9, 4);
    }

    @Test
    void initialViewportRendersVisibleContentRows() {
        PlayerMock player = server.addPlayer();
        TestScrollingMenu menu = new TestScrollingMenu(player, 2, List.of(0, 1, 2, 9, 10, 11));

        menu.nextLineButton(17, button(Material.EMERALD));
        menu.setContent(buttons(Material.DIAMOND, 8));
        menu.refresh();

        Inventory inventory = menu.getInventory();
        assertItem(inventory, 0, Material.DIAMOND);
        assertItem(inventory, 1, Material.DIAMOND);
        assertItem(inventory, 2, Material.DIAMOND);
        assertItem(inventory, 9, Material.DIAMOND);
        assertItem(inventory, 10, Material.DIAMOND);
        assertItem(inventory, 11, Material.DIAMOND);
        assertEquals(1, menu.getMaxLine());
        assertEquals(0, menu.getLine());
    }

    @Test
    void changingLineScrollsByOneContentRow() {
        PlayerMock player = server.addPlayer();
        TestScrollingMenu menu = new TestScrollingMenu(player, 2, List.of(0, 1, 2, 9, 10, 11));

        menu.previousLineButton(16, button(Material.REDSTONE));
        menu.nextLineButton(17, button(Material.EMERALD));
        menu.setContent(numberedButtons(8));
        menu.changeLine(1);

        Inventory inventory = menu.getInventory();
        assertItemAmount(inventory, 0, 4);
        assertItemAmount(inventory, 1, 5);
        assertItemAmount(inventory, 2, 6);
        assertItemAmount(inventory, 9, 7);
        assertItemAmount(inventory, 10, 8);
        assertNull(inventory.getItem(11));
        assertEquals(1, menu.getLine());
    }

    @Test
    void horizontalScrollingFillsColumnsAndMovesByOneColumn() {
        PlayerMock player = server.addPlayer();
        TestScrollingMenu menu = new TestScrollingMenu(player, 2, List.of(0, 1, 2, 9, 10, 11));

        menu.scrollDirection(ScrollDirection.HORIZONTAL);
        menu.previousLineButton(16, button(Material.REDSTONE));
        menu.nextLineButton(17, button(Material.EMERALD));
        menu.setContent(numberedButtons(8));
        menu.refresh();

        Inventory inventory = menu.getInventory();
        assertItemAmount(inventory, 0, 1);
        assertItemAmount(inventory, 9, 2);
        assertItemAmount(inventory, 1, 3);
        assertItemAmount(inventory, 10, 4);
        assertItemAmount(inventory, 2, 5);
        assertItemAmount(inventory, 11, 6);
        assertEquals(1, menu.getMaxLine());

        menu.changeLine(99);

        assertItemAmount(inventory, 0, 3);
        assertItemAmount(inventory, 9, 4);
        assertItemAmount(inventory, 1, 5);
        assertItemAmount(inventory, 10, 6);
        assertItemAmount(inventory, 2, 7);
        assertItemAmount(inventory, 11, 8);
        assertEquals(1, menu.getLine());
    }

    @Test
    void horizontalScrollingHonorsExplicitLineLength() {
        PlayerMock player = server.addPlayer();
        TestScrollingMenu menu = new TestScrollingMenu(player, 3, List.of(0, 9, 1, 10, 19, 2, 11, 20));

        menu.scrollDirection(ScrollDirection.HORIZONTAL);
        menu.previousLineButton(25, button(Material.REDSTONE));
        menu.nextLineButton(26, button(Material.EMERALD));
        menu.setLineLengthForTest(3);
        menu.setContent(numberedButtons(11));
        menu.changeLine(1);

        Inventory inventory = menu.getInventory();
        assertItemAmount(inventory, 0, 4);
        assertItemAmount(inventory, 9, 5);
        assertItemAmount(inventory, 1, 6);
        assertItemAmount(inventory, 10, 7);
        assertItemAmount(inventory, 19, 8);
        assertItemAmount(inventory, 2, 9);
        assertItemAmount(inventory, 11, 10);
        assertItemAmount(inventory, 20, 11);
        assertEquals(1, menu.getMaxLine());
    }

    @Test
    void horizontalShapeScrollsAcrossNineteenColumns() {
        PlayerMock player = server.addPlayer();
        TestScrollingMenu menu = new TestScrollingMenu(player, 4);

        menu.scrollDirection(ScrollDirection.HORIZONTAL);
        menu.contentShape(HORIZONTAL_SHAPE);
        menu.previousLineButton(27, button(Material.REDSTONE));
        menu.nextLineButton(35, button(Material.EMERALD));
        menu.setContent(numberedButtons(57));
        menu.refresh();

        Inventory inventory = menu.getInventory();
        assertItemAmount(inventory, 0, 1);
        assertItemAmount(inventory, 9, 2);
        assertItemAmount(inventory, 18, 3);
        assertItemAmount(inventory, 8, 25);
        assertEquals(10, menu.getMaxLine());

        menu.changeLine(10);

        assertItemAmount(inventory, 0, 31);
        assertItemAmount(inventory, 9, 32);
        assertItemAmount(inventory, 18, 33);
        assertItemAmount(inventory, 8, 55);
        assertEquals(10, menu.getLine());
    }

    @Test
    void verticalShapeScrollsAcrossNineRows() {
        PlayerMock player = server.addPlayer();
        TestScrollingMenu menu = new TestScrollingMenu(player, 6);

        menu.contentShape(VERTICAL_SHAPE);
        menu.previousLineButton(52, button(Material.REDSTONE));
        menu.nextLineButton(53, button(Material.EMERALD));
        menu.setContent(numberedButtons(81));
        menu.refresh();

        Inventory inventory = menu.getInventory();
        assertItemAmount(inventory, 0, 1);
        assertItemAmount(inventory, 8, 9);
        assertItemAmount(inventory, 45, 46);
        assertEquals(3, menu.getMaxLine());

        menu.changeLine(3);

        assertItemAmount(inventory, 0, 28);
        assertItemAmount(inventory, 8, 36);
        assertItemAmount(inventory, 27, 55);
        assertEquals(3, menu.getLine());
    }

    @Test
    void lineChangeClampsToAvailableContent() {
        PlayerMock player = server.addPlayer();
        TestScrollingMenu menu = new TestScrollingMenu(player, 2, List.of(0, 1, 2, 9, 10, 11));

        menu.previousLineButton(16, button(Material.REDSTONE));
        menu.setContent(buttons(Material.DIAMOND, 8));
        menu.changeLine(10);

        assertEquals(1, menu.getLine());
        assertEquals(1, menu.getMaxLine());
    }

    @Test
    void hiddenScrollControlsLeaveFixedButtonsVisible() {
        PlayerMock player = server.addPlayer();
        TestScrollingMenu menu = new TestScrollingMenu(player, 1, List.of(0, 1, 2));

        menu.addButton(2, button(Material.IRON_INGOT));
        menu.nextLineButton(2, button(Material.EMERALD));
        menu.setContent(List.of(button(Material.DIAMOND)));

        menu.refresh();

        assertItem(menu.getInventory(), 0, Material.DIAMOND);
        assertItem(menu.getInventory(), 2, Material.IRON_INGOT);
    }

    @Test
    void visibleScrollControlsOverrideFixedButtonsAndReserveContentSlots() {
        PlayerMock player = server.addPlayer();
        TestScrollingMenu menu = new TestScrollingMenu(player, 2, List.of(0, 1, 2, 9, 10, 11));

        menu.addButton(0, button(Material.IRON_INGOT));
        menu.addButton(11, button(Material.IRON_INGOT));
        menu.previousLineButton(0, button(Material.REDSTONE));
        menu.nextLineButton(11, button(Material.EMERALD));
        menu.setLineLengthForTest(2);
        menu.setContent(numberedButtons(10));

        menu.changeLine(1);

        Inventory inventory = menu.getInventory();
        assertItem(inventory, 0, Material.REDSTONE);
        assertItemAmount(inventory, 1, 3);
        assertItemAmount(inventory, 2, 4);
        assertItemAmount(inventory, 9, 5);
        assertItemAmount(inventory, 10, 6);
        assertItem(inventory, 11, Material.EMERALD);
    }

    @Test
    void contentButtonsKeepPersistentIdsAndRefreshTracking() {
        PlayerMock player = server.addPlayer();
        TestScrollingMenu menu = new TestScrollingMenu(player, 1, List.of(0, 1, 2));
        Button button = Button.builder()
                .item(viewer -> new ItemStack(Material.EMERALD))
                .refresh(20L)
                .build();

        menu.setContent(List.of(button));
        menu.refresh();

        Inventory inventory = menu.getInventory();
        ItemStack rendered = inventory.getItem(0);
        assertNotNull(rendered);
        ItemMeta meta = rendered.getItemMeta();
        assertNotNull(meta);
        assertEquals(button.uuid().toString(), meta.getPersistentDataContainer().get(Button.KEY, PersistentDataType.STRING));
        assertEquals(inventory, button.inventory());
        assertEquals(1, menu.getRefreshingContentButtons().size());
        assertTrue(menu.checkCancelClick(0));
    }

    @Test
    void scrollingUnbindsContentAndControlsThatLeaveViewport() {
        PlayerMock player = server.addPlayer();
        TestScrollingMenu menu = new TestScrollingMenu(player, 2, List.of(0, 1, 2, 9, 10, 11));
        Button previous = button(Material.REDSTONE);
        Button next = button(Material.EMERALD);
        List<Button> content = numberedButtons(8);
        Button first = content.getFirst();

        menu.previousLineButton(16, previous);
        menu.nextLineButton(17, next);
        menu.setContent(content);
        menu.refresh();

        Inventory inventory = menu.getInventory();
        assertEquals(inventory, first.inventory());
        assertEquals(inventory, next.inventory());
        assertNull(previous.inventory());

        menu.changeLine(1);

        assertNull(first.inventory());
        assertNull(next.inventory());
        assertEquals(inventory, previous.inventory());
    }

    private static List<Button> buttons(Material material, int count) {
        List<Button> buttons = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            buttons.add(button(material));
        }
        return buttons;
    }

    private static List<Button> numberedButtons(int count) {
        List<Button> buttons = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            int amount = i;
            buttons.add(Button.builder()
                    .item(player -> new ItemStack(Material.DIAMOND, amount))
                    .build());
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

    private static void assertItemAmount(Inventory inventory, int slot, int amount) {
        assertNotNull(inventory);
        ItemStack item = inventory.getItem(slot);
        assertNotNull(item);
        assertEquals(Material.DIAMOND, item.getType());
        assertEquals(amount, item.getAmount());
    }

    private static final class TestScrollingMenu extends ScrollingMenu {

        private TestScrollingMenu(Player player, int rows) {
            super(player, "Test", rows);
        }

        private TestScrollingMenu(Player player, int rows, List<Integer> contentSlots) {
            super(player, "Test", rows);
            setContentSlots(contentSlots);
        }

        private void contentShape(List<String> rows) {
            setContentShape(rows);
        }

        private void nextLineButton(int slot, Button button) {
            setNextLineButton(slot, button);
        }

        private void previousLineButton(int slot, Button button) {
            setPreviousLineButton(slot, button);
        }

        private void setLineLengthForTest(int lineLength) {
            setLineLength(lineLength);
        }

        private void scrollDirection(ScrollDirection scrollDirection) {
            setScrollDirection(scrollDirection);
        }
    }
}
