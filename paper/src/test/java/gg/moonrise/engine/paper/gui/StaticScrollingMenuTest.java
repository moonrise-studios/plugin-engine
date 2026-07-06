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
