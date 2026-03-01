package games.negative.engine.paper.gui.util;

import games.negative.engine.paper.gui.UserInterface;
import games.negative.engine.paper.gui.button.Button;
import games.negative.engine.paper.scheduler.Scheduler;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class MenuInteractionUtil {

    private MenuInteractionUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    public static void processClick(
            boolean cancelClicks,
            Map<UUID, Button> buttonById,
            Player player,
            InventoryClickEvent event
    ) {
        if (cancelClicks) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }

        ItemStack current = event.getCurrentItem();
        if (current == null) return;

        String id = current.getPersistentDataContainer().get(Button.KEY, PersistentDataType.STRING);
        if (id == null) return;

        Button button = buttonById.get(UUID.fromString(id));
        if (button == null) return;

        button.processClickAction(player, event);
    }

    public static void openMenu(
            Player player,
            UserInterface ui,
            Runnable refreshAction,
            Supplier<InventoryView> inventorySupplier
    ) {
        refreshAction.run();

        UserInterface.invalidateFromCache(player.getUniqueId());
        UserInterface.CACHE.put(player.getUniqueId(), ui);

        Scheduler.entity(player).execute(() -> {
            InventoryView view = inventorySupplier.get();
            if (view != null) view.open();
        }, 1);
    }

    public static void refreshButton(InventoryView inventory, int slot, Button button) {
        if (inventory == null) return;

        Player player = (Player) inventory.getPlayer();
        ItemStack stack = button.item(player);
        if (stack == null || stack.getType().isAir()) return;

        stack.editPersistentDataContainer(
                data -> data.set(Button.KEY, PersistentDataType.STRING, button.uuid().toString())
        );

        SafeUtil.setInventoryItem(inventory, slot, stack);
    }

    public static void addButton(
            int slot,
            Button button,
            Map<Integer, Button> buttons,
            Map<UUID, Button> buttonById,
            Map<Integer, Button> refreshingButtons
    ) {
        buttons.put(slot, button);
        buttonById.put(button.uuid(), button);

        if (button.refreshIntervalTicks() <= 0L) return;
        refreshingButtons.put(slot, button);
    }

    public static boolean checkCancelClick(InventoryView inventory, Map<UUID, Button> buttonById, int slot) {
        if (inventory == null) return false;

        ItemStack item = inventory.getItem(slot);
        if (item == null || item.getType().isAir()) return false;

        String id = item.getPersistentDataContainer().get(Button.KEY, PersistentDataType.STRING);
        if (id == null) return false;

        Button button = buttonById.get(UUID.fromString(id));
        if (button == null) return false;

        return button.cancelClick();
    }
}
