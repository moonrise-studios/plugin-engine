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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticScrollingMenuTest extends MockBukkitTest {

    private static final List<String> SAMPLE_LAYOUT = List.of(
            "# # # # i # # # #",
            "# # # # . # # # #",
            "# # # # . # # # #",
            "# # # # . # # # #",
            "# # # # . # # # #",
            "# # # # . # # # #",
            "# # a # # # b # #",
            "# # a # # # b # #",
            "# # a # # # b # #",
            "# # a # # # b # #",
            "# # a # # # b # #",
            "# # u # x # d # #"
    );

    private static final List<String> HORIZONTAL_LAYOUT = List.of(
            "u 1 2 3 4 5 6 7 8 9 A B C D E F G H d",
            "x x x x x x x x x x x x x x x x x x x",
            "# # # # # # # # # # # # # # # # # # #"
    );

    private static final List<String> VERTICAL_LAYOUT = List.of(
            "a a a a a a a a a",
            "b b b b b b b b b",
            "c c c c c c c c c",
            "d d d d d d d d d",
            "e e e e e e e e e",
            "f f f f f f f f f",
            "g g g g g g g g g",
            "h h h h h h h h h",
            "i i i i i i i i i"
    );

    @Test
    void staticLinesStayPinnedWhileLayoutLinesScroll() {
        PlayerMock player = server.addPlayer();
        TestStaticScrollingMenu menu = sampleMenu(player);

        menu.refresh();

        Inventory inventory = menu.getInventory();
        assertItem(inventory, 4, Material.BOOK);
        assertItem(inventory, 13, Material.DIAMOND);
        assertNull(inventory.getItem(47));
        assertItem(inventory, 49, Material.BARRIER);
        assertItem(inventory, 51, Material.ARROW);
        assertEquals(6, menu.getMaxLine());
        assertEquals(0, menu.getLine());

        menu.changeLine(5);

        assertItem(inventory, 4, Material.BOOK);
        assertItem(inventory, 11, Material.GOLD_INGOT);
        assertItem(inventory, 15, Material.LAPIS_LAZULI);
        assertItem(inventory, 47, Material.REDSTONE);
        assertItem(inventory, 49, Material.BARRIER);
        assertItem(inventory, 51, Material.ARROW);
        assertEquals(5, menu.getLine());

        menu.changeLine(20);

        assertEquals(6, menu.getLine());
        assertItem(inventory, 4, Material.BOOK);
        assertItem(inventory, 38, Material.GOLD_INGOT);
        assertItem(inventory, 42, Material.LAPIS_LAZULI);
        assertItem(inventory, 47, Material.REDSTONE);
        assertItem(inventory, 49, Material.BARRIER);
        assertNull(inventory.getItem(51));
    }

    @Test
    void scrollControlActionsCanUseLayoutSymbols() {
        PlayerMock player = server.addPlayer();
        TestStaticScrollingMenu menu = new TestStaticScrollingMenu(player, 6);
        Button up = Button.builder()
                .item(viewer -> new ItemStack(Material.REDSTONE))
                .action((button, viewer, event) -> menu.previousLine())
                .build();
        Button down = Button.builder()
                .item(viewer -> new ItemStack(Material.ARROW))
                .action((button, viewer, event) -> menu.nextLine())
                .build();

        menu.layout(SAMPLE_LAYOUT);
        menu.staticLines(0, 11);
        menu.button('i', button(Material.BOOK));
        menu.previousLineButton('u', up);
        menu.nextLineButton('d', down);
        menu.refresh();

        Inventory inventory = menu.getInventory();
        assertNull(up.inventory());
        assertEquals(inventory, down.inventory());

        ItemStack rendered = inventory.getItem(51);
        assertNotNull(rendered);
        ItemMeta meta = rendered.getItemMeta();
        assertNotNull(meta);
        assertEquals(down.uuid().toString(), meta.getPersistentDataContainer().get(Button.KEY, PersistentDataType.STRING));
        assertTrue(menu.checkCancelClick(51));

        down.processClickAction(player, null);

        assertEquals(1, menu.getLine());
        assertEquals(inventory, up.inventory());
    }

    @Test
    void fallbackSymbolsRenderWhenScrollControlsAreHidden() {
        PlayerMock player = server.addPlayer();
        TestStaticScrollingMenu menu = new TestStaticScrollingMenu(player, 6);

        menu.layout(SAMPLE_LAYOUT);
        menu.staticLines(0, 11);
        menu.button('#', button(Material.GRAY_STAINED_GLASS_PANE));
        menu.button('i', button(Material.BOOK));
        menu.button('.', button(Material.DIAMOND));
        menu.button('a', button(Material.GOLD_INGOT));
        menu.button('b', button(Material.LAPIS_LAZULI));
        menu.button('x', button(Material.BARRIER));
        menu.previousLineButton('u', '#', button(Material.REDSTONE));
        menu.nextLineButton('d', '#', button(Material.ARROW));

        menu.refresh();

        Inventory inventory = menu.getInventory();
        assertItem(inventory, 47, Material.GRAY_STAINED_GLASS_PANE);
        assertItem(inventory, 51, Material.ARROW);

        menu.changeLine(6);

        assertItem(inventory, 47, Material.REDSTONE);
        assertItem(inventory, 51, Material.GRAY_STAINED_GLASS_PANE);
    }

    @Test
    void compactLayoutLinesAreSupported() {
        PlayerMock player = server.addPlayer();
        TestStaticScrollingMenu menu = new TestStaticScrollingMenu(player, 3);

        menu.layout(List.of(
                "####i####",
                "####.####",
                "##u#x#d##"
        ));
        menu.staticLines(0, 2);
        menu.button('i', button(Material.BOOK));
        menu.button('.', button(Material.DIAMOND));
        menu.button('x', button(Material.BARRIER));
        menu.refresh();

        Inventory inventory = menu.getInventory();
        assertItem(inventory, 4, Material.BOOK);
        assertItem(inventory, 13, Material.DIAMOND);
        assertItem(inventory, 22, Material.BARRIER);
    }

    @Test
    void horizontalLayoutScrollsAcrossNineteenColumnsWithPinnedColumns() {
        PlayerMock player = server.addPlayer();
        TestStaticScrollingMenu menu = new TestStaticScrollingMenu(player, 3);

        menu.scrollDirection(ScrollDirection.HORIZONTAL);
        menu.layout(HORIZONTAL_LAYOUT);
        menu.staticLines(0, 18);
        menu.button('1', button(Material.DIAMOND));
        menu.button('B', button(Material.GOLD_INGOT));
        menu.button('H', button(Material.LAPIS_LAZULI));
        menu.button('x', button(Material.IRON_INGOT));
        menu.button('#', button(Material.GRAY_STAINED_GLASS_PANE));
        menu.previousLineButton('u', button(Material.REDSTONE));
        menu.nextLineButton('d', button(Material.ARROW));
        menu.refresh();

        Inventory inventory = menu.getInventory();
        assertEquals(ScrollDirection.HORIZONTAL, menu.getScrollDirection());
        assertNull(inventory.getItem(0));
        assertItem(inventory, 1, Material.DIAMOND);
        assertItem(inventory, 8, Material.ARROW);
        assertEquals(10, menu.getMaxLine());

        menu.changeLine(10);

        assertItem(inventory, 0, Material.REDSTONE);
        assertItem(inventory, 1, Material.GOLD_INGOT);
        assertItem(inventory, 7, Material.LAPIS_LAZULI);
        assertNull(inventory.getItem(8));
        assertEquals(10, menu.getLine());
    }

    @Test
    void verticalLayoutScrollsAcrossNineRows() {
        PlayerMock player = server.addPlayer();
        TestStaticScrollingMenu menu = new TestStaticScrollingMenu(player, 6);

        menu.layout(VERTICAL_LAYOUT);
        menu.button('a', button(Material.DIAMOND));
        menu.button('d', button(Material.GOLD_INGOT));
        menu.button('f', button(Material.IRON_INGOT));
        menu.button('i', button(Material.LAPIS_LAZULI));
        menu.refresh();

        Inventory inventory = menu.getInventory();
        assertEquals(ScrollDirection.VERTICAL, menu.getScrollDirection());
        assertItem(inventory, 0, Material.DIAMOND);
        assertItem(inventory, 45, Material.IRON_INGOT);
        assertEquals(3, menu.getMaxLine());

        menu.changeLine(3);

        assertItem(inventory, 0, Material.GOLD_INGOT);
        assertItem(inventory, 45, Material.LAPIS_LAZULI);
        assertEquals(3, menu.getLine());
    }

    private static TestStaticScrollingMenu sampleMenu(Player player) {
        TestStaticScrollingMenu menu = new TestStaticScrollingMenu(player, 6);
        menu.layout(SAMPLE_LAYOUT);
        menu.staticLines(0, 11);
        menu.button('i', button(Material.BOOK));
        menu.button('.', button(Material.DIAMOND));
        menu.button('a', button(Material.GOLD_INGOT));
        menu.button('b', button(Material.LAPIS_LAZULI));
        menu.button('x', button(Material.BARRIER));
        menu.previousLineButton('u', button(Material.REDSTONE));
        menu.nextLineButton('d', button(Material.ARROW));
        return menu;
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

    private static final class TestStaticScrollingMenu extends StaticScrollingMenu {

        private TestStaticScrollingMenu(Player player, int rows) {
            super(player, "Test", rows);
        }

        private void layout(List<String> layout) {
            setLayout(layout);
        }

        private void scrollDirection(ScrollDirection scrollDirection) {
            setScrollDirection(scrollDirection);
        }

        private void staticLines(Integer... lines) {
            setStaticLines(lines);
        }

        private void button(char symbol, Button button) {
            setButton(symbol, button);
        }

        private void nextLineButton(char symbol, Button button) {
            setNextLineButton(symbol, button);
        }

        private void nextLineButton(char symbol, char fallbackSymbol, Button button) {
            setNextLineButton(symbol, fallbackSymbol, button);
        }

        private void previousLineButton(char symbol, Button button) {
            setPreviousLineButton(symbol, button);
        }

        private void previousLineButton(char symbol, char fallbackSymbol, Button button) {
            setPreviousLineButton(symbol, fallbackSymbol, button);
        }
    }
}
