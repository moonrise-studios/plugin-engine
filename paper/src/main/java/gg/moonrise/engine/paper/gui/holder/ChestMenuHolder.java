package gg.moonrise.engine.paper.gui.holder;

import gg.moonrise.engine.paper.gui.ChestMenu;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public final class ChestMenuHolder implements InteractiveMenuHolder<ChestMenu> {

    private final ChestMenu menu;
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
    public ChestMenu getMenu() {
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
