package gg.moonrise.engine.paper.gui.button;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Represents an action that can be performed when a player clicks on a menu button.
 */
@FunctionalInterface
public interface ButtonClickAction {

    /**
     * Called when a player clicks on a menu button.
     * @param button the button that was clicked
     * @param player the player who clicked the button
     * @param event the inventory click event
     */
    void onClick(Button button, Player player, InventoryClickEvent event);

}
