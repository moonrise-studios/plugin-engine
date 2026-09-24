package gg.moonrise.engine.paper.gui.util;

import gg.moonrise.engine.paper.cooldown.Cooldowns;
import gg.moonrise.engine.paper.gui.button.Button;
import gg.moonrise.engine.paper.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

public final class MenuInteractionUtil {

    private MenuInteractionUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * Processes a menu click without cooldown handling.
     *
     * @param cancelClicks whether to cancel the inventory click
     * @param buttonById buttons keyed by their rendered identifiers
     * @param player player interacting with the menu
     * @param event inventory click event
     */
    public static void processClick(
            boolean cancelClicks,
            Map<UUID, Button> buttonById,
            Player player,
            InventoryClickEvent event
    ) {
        processClick(cancelClicks, buttonById, player, event, Duration.ZERO, "");
    }

    /**
     * Processes a menu click using optional per-menu cooldown state.
     *
     * @param cancelClicks whether to cancel the inventory click
     * @param buttonById buttons keyed by their rendered identifiers
     * @param player player interacting with the menu
     * @param event inventory click event
     * @param interactionCooldown non-negative interaction cooldown
     * @param interactionCooldownKey key unique to the menu instance
     */
    public static void processClick(
            boolean cancelClicks,
            Map<UUID, Button> buttonById,
            Player player,
            InventoryClickEvent event,
            Duration interactionCooldown,
            String interactionCooldownKey
    ) {
        Objects.requireNonNull(interactionCooldown, "interactionCooldown");
        Objects.requireNonNull(interactionCooldownKey, "interactionCooldownKey");
        if (interactionCooldown.isNegative()) {
            throw new IllegalArgumentException("Interaction cooldown cannot be negative.");
        }

        if (cancelClicks) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }

        ItemStack current = event.getCurrentItem();
        if (current == null) return;

        UUID id = buttonUuid(current);
        if (id == null) return;

        Button button = buttonById.get(id);
        if (button == null) return;
        if (button.clickAction() == null) return;

        if (!interactionCooldown.isZero()) {
            UUID playerUuid = player.getUniqueId();
            if (Cooldowns.isOnCooldown(playerUuid, interactionCooldownKey)) return;
            Cooldowns.addCooldown(playerUuid, interactionCooldownKey, interactionCooldown);
        }

        button.processClickAction(player, event);
    }

    public static void openMenu(
            Player player,
            Runnable refreshAction,
            Supplier<Inventory> inventorySupplier
    ) {
        refreshAction.run();

        Scheduler.entity(player).execute(() -> {
            Inventory inventory = inventorySupplier.get();
            if (inventory != null) player.openInventory(inventory);
        }, 1);
    }

    public static void refreshButton(Inventory inventory, int slot, Button button) {
        if (inventory == null) return;
        if (inventory.getViewers().isEmpty()) return;

        for (var viewer : inventory.getViewers()) {
            if (viewer instanceof Player player) {
                refreshButton(inventory, slot, button, player);
                return;
            }
        }
    }

    public static boolean refreshButton(Inventory inventory, int slot, Button button, Player player) {
        return renderButton(inventory, slot, button, player);
    }

    public static boolean renderButton(Inventory inventory, int slot, Button button, Player player) {
        if (inventory == null || button == null || player == null) return false;
        if (slot < 0 || slot >= inventory.getSize()) {
            button.onAddToInventory(null);
            return false;
        }

        ItemStack stack = button.item(player);
        if (stack == null || stack.getType().isAir()) {
            SafeUtil.setInventoryItem(inventory, slot, null);
            button.onAddToInventory(null);
            return false;
        }

        tagButtonItem(stack, button.uuid());

        SafeUtil.setInventoryItem(inventory, slot, stack);
        button.onAddToInventory(inventory, slot);
        return true;
    }

    public static void addButton(
            int slot,
            Button button,
            Map<Integer, Button> buttons,
            Map<UUID, Button> buttonById,
            Map<Integer, Button> refreshingButtons
    ) {
        Button replaced = buttons.put(slot, button);
        if (replaced != null && replaced != button) {
            buttonById.remove(replaced.uuid());
            refreshingButtons.remove(slot);
            replaced.onAddToInventory(null);
        }
        buttonById.put(button.uuid(), button);

        if (button.refreshIntervalTicks() > 0L) {
            refreshingButtons.put(slot, button);
        }
    }

    public static boolean checkCancelClick(Inventory inventory, Map<UUID, Button> buttonById, int slot) {
        if (inventory == null) return false;

        ItemStack item = inventory.getItem(slot);
        if (item == null || item.getType().isAir()) return false;

        UUID id = buttonUuid(item);
        if (id == null) return false;

        Button button = buttonById.get(id);
        if (button == null) return false;

        return button.cancelClick();
    }

    public static void tagButtonItem(ItemStack stack, UUID uuid) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().set(Button.KEY, PersistentDataType.STRING, uuid.toString());
        stack.setItemMeta(meta);
    }

    public static String buttonId(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;

        return meta.getPersistentDataContainer().get(Button.KEY, PersistentDataType.STRING);
    }

    public static UUID buttonUuid(ItemStack stack) {
        String id = buttonId(stack);
        if (id == null) return null;

        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
