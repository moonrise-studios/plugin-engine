package gg.moonrise.engine.paper.gui.holder;

import gg.moonrise.engine.paper.gui.UserInterface;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;

public interface InteractiveMenuHolder<T extends UserInterface> extends InventoryHolder {

    void onOpen(Player player, InventoryOpenEvent event);

    void onClose(Player player, InventoryCloseEvent event);

    void onClick(Player player, InventoryClickEvent event);

    T getMenu();
}
