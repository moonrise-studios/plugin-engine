package gg.moonrise.engine.paper.gui.controller;

import gg.moonrise.engine.paper.gui.button.Button;
import gg.moonrise.engine.paper.gui.UserInterface;
import gg.moonrise.engine.paper.gui.holder.InteractiveMenuHolder;
import gg.moonrise.engine.paper.gui.util.MenuInteractionUtil;
import gg.moonrise.engine.paper.job.SyncJob;
import gg.moonrise.engine.paper.scheduler.Scheduler;
import gg.moonrise.moss.spring.SpringComponent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the PlayerInventoryController class.
 */

@SpringComponent
public class PlayerInventoryController implements Listener, SyncJob {

    private final Map<UUID, TrackedMenu> openMenus = new ConcurrentHashMap<>();
    private long tick;

    /**
     * Executes onOpen.
     * @param event the event
     */

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        HumanEntity client = event.getPlayer();

        InteractiveMenuHolder<?> holder = getMenuHolder(event.getInventory().getHolder());
        if (holder == null) return;

        Player player = (Player) client;
        openMenus.put(player.getUniqueId(), new TrackedMenu(player, holder.getMenu()));
        holder.onOpen(player, event);
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

        Player player = (Player) client;
        UserInterface menu = holder.getMenu();
        openMenus.computeIfPresent(player.getUniqueId(), (uuid, tracked) -> tracked.menu() == menu ? null : tracked);
        holder.onClose(player, event);
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

    @Override
    public Duration interval() {
        return Duration.ofMillis(50);
    }

    @Override
    public Duration delay() {
        return Duration.ofMillis(50);
    }

    @Override
    public void tick(ScheduledTask task) {
        long currentTick = ++tick;
        openMenus.values().forEach(tracked -> scheduleRefresh(tracked, currentTick));
    }

    private void scheduleRefresh(TrackedMenu tracked, long currentTick) {
        boolean scheduled = Scheduler.entity(tracked.player()).execute(
                () -> refreshTrackedMenu(tracked, currentTick),
                () -> openMenus.remove(tracked.player().getUniqueId(), tracked),
                0L
        );

        if (!scheduled) {
            openMenus.remove(tracked.player().getUniqueId(), tracked);
        }
    }

    private void refreshTrackedMenu(TrackedMenu tracked, long currentTick) {
        Player player = tracked.player();
        UserInterface menu = tracked.menu();
        Inventory inventory = menu.getInventory();

        if (inventory == null || !player.getOpenInventory().getTopInventory().equals(inventory)) {
            openMenus.remove(player.getUniqueId(), tracked);
            return;
        }

        Collection<Map.Entry<Integer, Button>> fixedButtons = menu.getRefreshingButtons();
        Collection<Map.Entry<Integer, Button>> contentButtons = menu.getRefreshingContentButtons();
        if (fixedButtons.isEmpty() && contentButtons.isEmpty()) return;

        var refreshedButtons = Collections.newSetFromMap(new IdentityHashMap<Button, Boolean>());
        refreshDueButtons(inventory, player, fixedButtons, currentTick, refreshedButtons);
        refreshDueButtons(inventory, player, contentButtons, currentTick, refreshedButtons);
        refreshedButtons.forEach(button -> button.setLastRefreshTime(currentTick));
    }

    private void refreshDueButtons(
            Inventory inventory,
            Player player,
            Collection<Map.Entry<Integer, Button>> buttons,
            long currentTick,
            Collection<Button> refreshedButtons
    ) {
        for (Map.Entry<Integer, Button> entry : buttons) {
            Button button = entry.getValue();
            long interval = button.refreshIntervalTicks();
            if (interval <= 0L) continue;
            if (currentTick - button.lastRefreshTime() < interval) continue;

            MenuInteractionUtil.refreshButton(inventory, entry.getKey(), button, player);
            refreshedButtons.add(button);
        }
    }

    private InteractiveMenuHolder<?> getMenuHolder(InventoryHolder holder) {
        if (!(holder instanceof InteractiveMenuHolder<?> menuHolder)) return null;
        return menuHolder;
    }

    private record TrackedMenu(Player player, UserInterface menu) {
    }

}
