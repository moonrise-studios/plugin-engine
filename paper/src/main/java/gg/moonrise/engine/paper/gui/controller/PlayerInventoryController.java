package gg.moonrise.engine.paper.gui.controller;

import gg.moonrise.engine.paper.gui.UserInterface;
import gg.moonrise.engine.paper.gui.holder.InteractiveMenuHolder;
import gg.moonrise.moss.spring.SpringComponent;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;

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

        InteractiveMenuHolder<?> holder = getMenuHolder(event.getInventory().getHolder());
        if (holder == null) return;

        holder.onOpen((Player) client, event);
    }


    /**
     * Executes onClose.
     * @param event the event
     */

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        HumanEntity client = event.getPlayer();

        InteractiveMenuHolder<?> holder = getMenuHolder(event.getInventory().getHolder());
        if (holder == null) return;

        holder.onClose((Player) client, event);
    }


    /**
     * Executes onClick.
     * @param event the event
     */

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        HumanEntity client = event.getWhoClicked();

        InteractiveMenuHolder<?> holder = getMenuHolder(event.getInventory().getHolder());
        if (holder == null) return;

        UserInterface ui = holder.getMenu();

        if (ui.cancelClicks() || ui.checkCancelClick(event.getSlot())) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }

        holder.onClick((Player) client, event);
    }

    private InteractiveMenuHolder<?> getMenuHolder(InventoryHolder holder) {
        if (!(holder instanceof InteractiveMenuHolder<?> menuHolder)) return null;
        return menuHolder;
    }

}
