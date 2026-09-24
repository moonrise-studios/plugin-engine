package gg.moonrise.engine.paper.gui.util;

import gg.moonrise.engine.paper.gui.button.Button;
import gg.moonrise.engine.paper.support.MockBukkitTest;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MenuInteractionUtilTest extends MockBukkitTest {

    @Test
    void cooldownStartsOnlyForButtonsWithActions() {
        PlayerMock player = server.addPlayer();
        Inventory inventory = Bukkit.createInventory(null, 9);
        Map<UUID, Button> buttons = new HashMap<>();
        AtomicInteger interactions = new AtomicInteger();

        Button actionless = Button.of(viewer -> new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        Button actionable = Button.builder()
                .item(viewer -> new ItemStack(Material.EMERALD))
                .action((button, viewer, event) -> interactions.incrementAndGet())
                .build();
        buttons.put(actionless.uuid(), actionless);
        buttons.put(actionable.uuid(), actionable);
        MenuInteractionUtil.renderButton(inventory, 0, actionless, player);
        MenuInteractionUtil.renderButton(inventory, 1, actionable, player);
        player.openInventory(inventory);

        MenuInteractionUtil.processClick(true, buttons, player, click(player, 0), Duration.ofMinutes(1), "action-only");
        MenuInteractionUtil.processClick(true, buttons, player, click(player, 1), Duration.ofMinutes(1), "action-only");
        MenuInteractionUtil.processClick(true, buttons, player, click(player, 1), Duration.ofMinutes(1), "action-only");

        assertEquals(1, interactions.get());
    }

    @Test
    void cooldownStateIsIsolatedPerMenuInstance() {
        PlayerMock player = server.addPlayer();
        Inventory inventory = Bukkit.createInventory(null, 9);
        Map<UUID, Button> buttons = new HashMap<>();
        AtomicInteger interactions = new AtomicInteger();
        Button button = Button.builder()
                .item(viewer -> new ItemStack(Material.DIAMOND))
                .action((clicked, viewer, event) -> interactions.incrementAndGet())
                .build();
        buttons.put(button.uuid(), button);
        MenuInteractionUtil.renderButton(inventory, 0, button, player);
        player.openInventory(inventory);

        MenuInteractionUtil.processClick(true, buttons, player, click(player, 0), Duration.ofMinutes(1), "menu-one");
        MenuInteractionUtil.processClick(true, buttons, player, click(player, 0), Duration.ofMinutes(1), "menu-two");

        assertEquals(2, interactions.get());
    }

    @Test
    void unresolvedItemsDoNotStartCooldown() {
        PlayerMock player = server.addPlayer();
        Inventory inventory = Bukkit.createInventory(null, 9);
        Map<UUID, Button> buttons = new HashMap<>();
        AtomicInteger interactions = new AtomicInteger();
        Button actionable = Button.builder()
                .item(viewer -> new ItemStack(Material.EMERALD))
                .action((button, viewer, event) -> interactions.incrementAndGet())
                .build();
        buttons.put(actionable.uuid(), actionable);

        ItemStack invalid = new ItemStack(Material.PAPER);
        ItemMeta invalidMeta = invalid.getItemMeta();
        invalidMeta.getPersistentDataContainer().set(Button.KEY, PersistentDataType.STRING, "not-a-uuid");
        invalid.setItemMeta(invalidMeta);
        inventory.setItem(0, invalid);

        ItemStack unregistered = new ItemStack(Material.BOOK);
        MenuInteractionUtil.tagButtonItem(unregistered, UUID.randomUUID());
        inventory.setItem(1, unregistered);
        MenuInteractionUtil.renderButton(inventory, 2, actionable, player);
        player.openInventory(inventory);

        MenuInteractionUtil.processClick(true, buttons, player, click(player, 0), Duration.ofMinutes(1), "unresolved");
        MenuInteractionUtil.processClick(true, buttons, player, click(player, 1), Duration.ofMinutes(1), "unresolved");
        MenuInteractionUtil.processClick(true, buttons, player, click(player, 3), Duration.ofMinutes(1), "unresolved");
        MenuInteractionUtil.processClick(true, buttons, player, click(player, 2), Duration.ofMinutes(1), "unresolved");

        assertEquals(1, interactions.get());
    }

    @Test
    void cooldownIsAcquiredBeforeActionRuns() {
        PlayerMock player = server.addPlayer();
        Inventory inventory = Bukkit.createInventory(null, 9);
        Map<UUID, Button> buttons = new HashMap<>();
        AtomicInteger interactions = new AtomicInteger();
        Button failing = Button.builder()
                .item(viewer -> new ItemStack(Material.REDSTONE))
                .action((button, viewer, event) -> {
                    throw new IllegalStateException("failure");
                })
                .build();
        Button actionable = Button.builder()
                .item(viewer -> new ItemStack(Material.EMERALD))
                .action((button, viewer, event) -> interactions.incrementAndGet())
                .build();
        buttons.put(failing.uuid(), failing);
        buttons.put(actionable.uuid(), actionable);
        MenuInteractionUtil.renderButton(inventory, 0, failing, player);
        MenuInteractionUtil.renderButton(inventory, 1, actionable, player);
        player.openInventory(inventory);

        assertThrows(
                IllegalStateException.class,
                () -> MenuInteractionUtil.processClick(
                        true, buttons, player, click(player, 0), Duration.ofMinutes(1), "failing"
                )
        );
        MenuInteractionUtil.processClick(
                true, buttons, player, click(player, 1), Duration.ofMinutes(1), "failing"
        );

        assertEquals(0, interactions.get());
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
}
