package gg.moonrise.engine.paper.gui.controller;

import gg.moonrise.engine.paper.gui.ChestMenu;
import gg.moonrise.engine.paper.gui.button.Button;
import gg.moonrise.engine.paper.support.MockBukkitTest;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerInventoryControllerTest extends MockBukkitTest {

    private static final int HOTBAR_FIRST_RAW_SLOT = 54;
    private static final int PLAYER_SLOT_27_RAW_SLOT = 45;

    private final PlayerInventoryController controller = new PlayerInventoryController();
    private PlayerMock player;
    private TestChestMenu menu;

    @BeforeEach
    void openEditableMenu() {
        player = server.addPlayer();
        menu = new TestChestMenu(player, 3);
        menu.cancelClicks(false);
        menu.addButton(0, Button.of(viewer -> new ItemStack(Material.DIAMOND)));
        menu.refresh();
        player.openInventory(menu.getInventory());
    }

    @Test
    void playerInventoryClickPastMenuSizeIsAllowed() {
        InventoryClickEvent event = click(InventoryType.SlotType.CONTAINER, PLAYER_SLOT_27_RAW_SLOT);

        assertDoesNotThrow(() -> controller.onClick(event));
        assertFalse(event.isCancelled());
    }

    @Test
    void playerInventoryClickSharingAButtonSlotIndexIsAllowed() {
        InventoryClickEvent event = click(InventoryType.SlotType.QUICKBAR, HOTBAR_FIRST_RAW_SLOT);

        controller.onClick(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void clickOutsideTheWindowIsAllowed() {
        InventoryClickEvent event = click(InventoryType.SlotType.OUTSIDE, InventoryView.OUTSIDE);

        assertDoesNotThrow(() -> controller.onClick(event));
        assertFalse(event.isCancelled());
    }

    @Test
    void menuButtonClickIsCancelled() {
        InventoryClickEvent event = click(InventoryType.SlotType.CONTAINER, 0);

        controller.onClick(event);

        assertTrue(event.isCancelled());
    }

    private InventoryClickEvent click(InventoryType.SlotType slotType, int rawSlot) {
        return new InventoryClickEvent(
                player.getOpenInventory(),
                slotType,
                rawSlot,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        );
    }

    private static final class TestChestMenu extends ChestMenu {

        private TestChestMenu(Player player, int rows) {
            super(player, "Test", rows);
        }
    }
}
