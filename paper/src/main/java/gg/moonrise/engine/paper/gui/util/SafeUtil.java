package gg.moonrise.engine.paper.gui.util;

import lombok.extern.slf4j.Slf4j;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Utility class for safely interacting with inventories.
 */
@Slf4j
public class SafeUtil {

    /**
     * Sets an item in the inventory at the specified slot safely.
     * @param inventory the inventory to set the item in
     * @param slot the slot to set the item in
     * @param item the item to set
     */
    public static void setInventoryItem(Inventory inventory, int slot, ItemStack item) {
        if (inventory == null) return;
        if (slot < 0 || slot >= inventory.getSize()) {
            log.warn("Invalid inventory slot {} for inventory size {}", slot, inventory.getSize());
            return;
        }

        inventory.setItem(slot, item);
    }

}
