package games.negative.engine.paper.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryView;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents the UserInterface interface.
 */

public interface UserInterface {

    Map<UUID, UserInterface> CACHE = new HashMap<>();

    /**
     * Called when a player opens an inventory.
     *
     * @param player The player who opened the inventory.
     * @param event The InventoryOpenEvent triggered by the player opening the inventory.
     */
    void onOpen(Player player, InventoryOpenEvent event);

    /**
     * Called when the player closes the inventory associated with this menu.
     *
     * @param player the player who closed the inventory
     * @param event the close event
     */
    void onClose(Player player, InventoryCloseEvent event);

    /**
     * Called when the player clicks on the inventory.
     *
     * @param player the player who clicked on the inventory
     * @param event the inventory click event
     */
    void onClick(Player player, InventoryClickEvent event);

    /**
     * Opens an interactive menu for the given player.
     */
    void open();

    /**
     * Refreshes the inventory of the InteractiveMenu for the specified player.
     */
    void refresh();

    /**
     * Gets the InventoryView associated with this UserInterface.
     *
     * @return The InventoryView of this UserInterface.
     */
    InventoryView getView();

    /**
     * Clears the inventory view by setting all items in the top inventory to null.
     * @param view The InventoryView to clear.
     */
    default void clearInventory(InventoryView view) {
        for (int i = 0; i < view.getTopInventory().getSize(); i++) {
            view.getTopInventory().setItem(i, null);
        }
    }

    /**
     * Determines whether clicks in the inventory should be cancelled.
     * @return true if clicks should be cancelled, false otherwise.
     */
    boolean cancelClicks();

    /**
     * Checks if a click in a specific slot should be cancelled.
     * @param slot The slot number to check.
     * @return true if the click should be cancelled, false otherwise.
     */
    boolean checkCancelClick(int slot);

    /**
     * Invalidate the current state of the UserInterface.
     */
    void invalidate();

    static void invalidateFromCache(UUID uuid) {
        UserInterface ui = CACHE.remove(uuid);
        if (ui == null) return;

        ui.invalidate();
    }
}
