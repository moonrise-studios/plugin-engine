package gg.moonrise.engine.paper.gui.controller;

import gg.moonrise.engine.paper.gui.UserInterface;
import gg.moonrise.moss.spring.SpringComponent;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryView;

/**
 * Represents the PlayerInventoryController class.
 */

@SpringComponent
public class PlayerInventoryController implements Listener {

    /**
     * Executes onOpen.
     * @param event the event
     */

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        HumanEntity client = event.getPlayer();

        UserInterface ui = getUserInterface(client, event);
        if (ui == null) return;

        ui.onOpen((Player) client, event);
    }


    /**
     * Executes onClose.
     * @param event the event
     */

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        HumanEntity client = event.getPlayer();

        UserInterface ui = getUserInterface(client, event);
        if (ui == null) return;

        ui.onClose((Player) client, event);
    }


    /**
     * Executes onClick.
     * @param event the event
     */

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        HumanEntity client = event.getWhoClicked();

        UserInterface ui = getUserInterface(client, event);
        if (ui == null) return;

        if (ui.cancelClicks() || ui.checkCancelClick(event.getSlot())) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }

        ui.onClick((Player) client, event);
    }

    /**
     * Retrieves the UserInterface associated with the given HumanEntity and InventoryEvent.
     *
     * @param client The HumanEntity (player) involved in the event.
     * @param event  The InventoryEvent to check against.
     * @return The UserInterface if it exists and matches the event's inventory view; otherwise, null.
     */
    private UserInterface getUserInterface(HumanEntity client, InventoryEvent event) {
        UserInterface ui = UserInterface.CACHE.get(client.getUniqueId());
        if (ui == null) return null;
        if (ui.getView() == null) {
            UserInterface.invalidateFromCache(client.getUniqueId());
            return null;
        }

        InventoryView inventory = event.getView();
        if (!ui.getView().equals(inventory)) {
            UserInterface.invalidateFromCache(client.getUniqueId());
            return null;
        }

        return ui;
    }

}
