package gg.moonrise.engine.paper.gui.util;

import lombok.extern.slf4j.Slf4j;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

/**
 * Utility class for safely interacting with inventories.
 */
@Slf4j
public class SafeUtil {

    /**
     * Sets an item in the inventory at the specified slot safely.
     * @param view the inventory to set the item in
     * @param slot the slot to set the item in
     * @param item the item to set
     */
    public static void setInventoryItem(InventoryView view, int slot, ItemStack item) {
        if (view == null) return;
        if (slot < 0 || slot >= view.getTopInventory().getSize()) {
            log.warn("Invalid inventory slot {} for inventory size {}", slot, view.getTopInventory().getSize());
            return;
        }

        view.getTopInventory().setItem(slot, item);
    }

}
