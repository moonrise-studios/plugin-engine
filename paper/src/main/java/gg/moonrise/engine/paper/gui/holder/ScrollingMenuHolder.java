package gg.moonrise.engine.paper.gui.holder;

import gg.moonrise.engine.paper.gui.ScrollingMenu;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public final class ScrollingMenuHolder implements InteractiveMenuHolder<ScrollingMenu> {

    private final ScrollingMenu menu;
    private Inventory inventory;

    @Override
    public void onOpen(Player player, InventoryOpenEvent event) {
        menu.onOpen(player, event);
    }

    @Override
    public void onClose(Player player, InventoryCloseEvent event) {
        menu.onClose(player, event);
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        menu.onClick(player, event);
    }

    @Override
    public ScrollingMenu getMenu() {
        return menu;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
