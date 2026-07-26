package gg.moonrise.engine.paper.gui;

import gg.moonrise.engine.paper.gui.button.Button;
import gg.moonrise.engine.paper.support.MockBukkitTest;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MenuInteractionCooldownIntegrationTest extends MockBukkitTest {

    @Test
    void defaultCooldownBlocksRepeatedChestMenuAction() {
        PlayerMock player = server.addPlayer();
        TestChestMenu menu = new TestChestMenu(player);
        AtomicInteger interactions = new AtomicInteger();
        menu.addButton(0, actionButton(interactions));

        assertEquals(Duration.ofMillis(250), UserInterface.DEFAULT_INTERACTION_COOLDOWN);
        assertRepeatedClicks(menu, player, 0, interactions, 1);
    }

    @Test
    void zeroCooldownWorksAcrossEveryMenuType() {
        AtomicInteger interactions = new AtomicInteger();

        PlayerMock chestPlayer = server.addPlayer();
        TestChestMenu chestMenu = new TestChestMenu(chestPlayer);
        chestMenu.setInteractionCooldown(Duration.ZERO);
        chestMenu.addButton(0, actionButton(interactions));
        assertRepeatedClicks(chestMenu, chestPlayer, 0, interactions, 2);

        PlayerMock hopperPlayer = server.addPlayer();
        TestHopperMenu hopperMenu = new TestHopperMenu(hopperPlayer);
        hopperMenu.setInteractionCooldown(Duration.ZERO);
        hopperMenu.addButton(0, actionButton(interactions));
        assertRepeatedClicks(hopperMenu, hopperPlayer, 0, interactions, 4);

        PlayerMock paginatedPlayer = server.addPlayer();
        TestPaginatedMenu paginatedMenu = new TestPaginatedMenu(paginatedPlayer);
        paginatedMenu.setInteractionCooldown(Duration.ZERO);
        paginatedMenu.addButton(0, actionButton(interactions));
        assertRepeatedClicks(paginatedMenu, paginatedPlayer, 0, interactions, 6);

        PlayerMock scrollingPlayer = server.addPlayer();
        TestScrollingMenu scrollingMenu = new TestScrollingMenu(scrollingPlayer);
        scrollingMenu.setInteractionCooldown(Duration.ZERO);
        scrollingMenu.addButton(0, actionButton(interactions));
        assertRepeatedClicks(scrollingMenu, scrollingPlayer, 0, interactions, 8);

        PlayerMock staticPlayer = server.addPlayer();
        TestStaticScrollingMenu staticMenu = new TestStaticScrollingMenu(staticPlayer);
        staticMenu.setInteractionCooldown(Duration.ZERO);
        staticMenu.setLayout(List.of("x x x x x x x x x"));
        staticMenu.setButton('x', actionButton(interactions));
        assertRepeatedClicks(staticMenu, staticPlayer, 0, interactions, 10);
    }

    @Test
    void changingDurationClearsActiveCooldownAndRejectsInvalidValues() {
        PlayerMock player = server.addPlayer();
        TestChestMenu menu = new TestChestMenu(player);
        AtomicInteger interactions = new AtomicInteger();
        menu.addButton(0, actionButton(interactions));
        menu.refresh();
        player.openInventory(menu.getInventory());

        menu.onClick(player, click(player, 0));
        menu.setInteractionCooldown(Duration.ZERO);
        menu.onClick(player, click(player, 0));

        assertEquals(2, interactions.get());
        assertThrows(NullPointerException.class, () -> menu.setInteractionCooldown(null));
        assertThrows(IllegalArgumentException.class, () -> menu.setInteractionCooldown(Duration.ofNanos(-1)));
    }

    private static void assertRepeatedClicks(
            UserInterface menu,
            PlayerMock player,
            int slot,
            AtomicInteger interactions,
            int expectedInteractions
    ) {
        menu.refresh();
        player.openInventory(menu.getInventory());

        menu.onClick(player, click(player, slot));
        menu.onClick(player, click(player, slot));

        assertEquals(expectedInteractions, interactions.get());
    }

    private static Button actionButton(AtomicInteger interactions) {
        return Button.builder()
                .item(viewer -> new ItemStack(Material.EMERALD))
                .action((button, viewer, event) -> interactions.incrementAndGet())
                .build();
    }

    private static InventoryClickEvent click(PlayerMock player, int slot) {
        return new InventoryClickEvent(
                player.getOpenInventory(),
                InventoryType.SlotType.CONTAINER,
                slot,
                ClickType.LEFT,
                InventoryAction.PICKUP_ALL
        );
    }

    private static final class TestChestMenu extends ChestMenu {
        private TestChestMenu(Player player) {
            super(player, "Test", 1);
        }
    }

    private static final class TestHopperMenu extends HopperMenu {
        private TestHopperMenu(Player player) {
            super(player, "Test");
        }
    }

    private static final class TestPaginatedMenu extends PaginatedMenu {
        private TestPaginatedMenu(Player player) {
            super(player, "Test", 1);
        }
    }

    private static final class TestScrollingMenu extends ScrollingMenu {
        private TestScrollingMenu(Player player) {
            super(player, "Test", 1);
        }
    }

    private static final class TestStaticScrollingMenu extends StaticScrollingMenu {
        private TestStaticScrollingMenu(Player player) {
            super(player, "Test", 1);
        }
    }
}
