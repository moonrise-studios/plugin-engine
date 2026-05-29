package gg.moonrise.engine.paper.packet;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetCursorItem;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import gg.moonrise.engine.paper.PaperPlugin;
import gg.moonrise.engine.paper.event.PlayerViewItemEvent;
import gg.moonrise.moss.spring.Disableable;
import gg.moonrise.moss.spring.Enableable;
import gg.moonrise.moss.spring.SpringComponent;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Owns PacketEvents item-stack packet interception for PlayerViewItemEvent.
 */
@Slf4j
@SpringComponent
@RequiredArgsConstructor
public final class PlayerViewItemPacketService implements Enableable, Disableable, Listener {

    private final PaperPlugin plugin;
    private Object registeredListener;
    private PlayerViewItemPacketListener listener;

    @Override
    public void onEnable() {
        if (registeredListener != null) return;

        if (!isPacketEventsEnabled()) {
            log.warn("PacketEvents is not available; packet-based functionality will not work.");
            return;
        }

        listener = new PlayerViewItemPacketListener(plugin);
        registeredListener = PacketEvents.getAPI().getEventManager().registerListener(listener, PacketListenerPriority.NORMAL);
    }

    @Override
    public void onDisable() {
        if (registeredListener == null) return;
        PacketEvents.getAPI().getEventManager().unregisterListener((PacketListenerCommon) registeredListener);
        listener.clearCache();
        registeredListener = null;
        listener = null;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (listener == null) return;
        listener.clearCache(event.getPlayer().getUniqueId());
    }

    private boolean isPacketEventsEnabled() {
        PluginManager pluginManager = Bukkit.getPluginManager();
        return pluginManager.isPluginEnabled("PacketEvents") || pluginManager.isPluginEnabled("packetevents");
    }

    private static final class PlayerViewItemPacketListener implements PacketListener {

        private final PaperPlugin plugin;
        private final ClientItemCache<com.github.retrooper.packetevents.protocol.item.ItemStack> cache =
                new ClientItemCache<>(
                        com.github.retrooper.packetevents.protocol.item.ItemStack::isEmpty,
                        com.github.retrooper.packetevents.protocol.item.ItemStack::copy
                );

        private PlayerViewItemPacketListener(PaperPlugin plugin) {
            this.plugin = plugin;
        }

        private void clearCache() {
            cache.clear();
        }

        private void clearCache(UUID playerId) {
            cache.clear(playerId);
        }

        @Override
        public void onPacketSend(PacketSendEvent event) {
            Object rawPlayer = event.getPlayer();
            if (!(rawPlayer instanceof Player player)) return;

            switch (event.getPacketType()) {
                case PacketType.Play.Server.SET_SLOT -> handleSetSlot(event, player);

                case PacketType.Play.Server.WINDOW_ITEMS -> handleWindowItems(event, player);

                case PacketType.Play.Server.SET_CURSOR_ITEM -> handleSetCursorItem(event, player);

                case PacketType.Play.Server.SET_PLAYER_INVENTORY -> handleSetPlayerInventory(event, player);

                default -> {
                }
            }
        }

        private void handleSetSlot(PacketSendEvent event, Player player) {
            WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
            com.github.retrooper.packetevents.protocol.item.ItemStack replacement = resolveClientItem(
                    player,
                    wrapper.getItem(),
                    PlayerViewItemEvent.Source.SET_SLOT,
                    wrapper.getWindowId(),
                    wrapper.getSlot(),
                    wrapper.getStateId()
            );

            if (replacement == null) return;
            wrapper.setItem(replacement);
            event.markForReEncode(true);
        }

        private void handleWindowItems(PacketSendEvent event, Player player) {
            WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);
            List<com.github.retrooper.packetevents.protocol.item.ItemStack> items = wrapper.getItems();
            List<com.github.retrooper.packetevents.protocol.item.ItemStack> replacedItems = null;
            boolean changed = false;

            for (int slot = 0; slot < items.size(); slot++) {
                com.github.retrooper.packetevents.protocol.item.ItemStack replacement = resolveClientItem(
                        player,
                        items.get(slot),
                        PlayerViewItemEvent.Source.WINDOW_ITEMS,
                        wrapper.getWindowId(),
                        slot,
                        wrapper.getStateId()
                );
                if (replacement == null) continue;

                if (replacedItems == null) replacedItems = new ArrayList<>(items);
                replacedItems.set(slot, replacement);
                changed = true;
            }

            if (changed) {
                wrapper.setItems(replacedItems);
            }

            if (wrapper.getCarriedItem().isPresent()) {
                com.github.retrooper.packetevents.protocol.item.ItemStack replacement = resolveClientItem(
                        player,
                        wrapper.getCarriedItem().get(),
                        PlayerViewItemEvent.Source.WINDOW_CARRIED_ITEM,
                        wrapper.getWindowId(),
                        -1,
                        wrapper.getStateId()
                );
                if (replacement != null) {
                    wrapper.setCarriedItem(replacement);
                    changed = true;
                }
            }

            if (changed) {
                event.markForReEncode(true);
            }
        }

        private void handleSetCursorItem(PacketSendEvent event, Player player) {
            WrapperPlayServerSetCursorItem wrapper = new WrapperPlayServerSetCursorItem(event);
            com.github.retrooper.packetevents.protocol.item.ItemStack replacement = resolveClientItem(
                    player,
                    wrapper.getStack(),
                    PlayerViewItemEvent.Source.SET_CURSOR_ITEM,
                    -1,
                    -1,
                    -1
            );

            if (replacement == null) return;
            wrapper.setStack(replacement);
            event.markForReEncode(true);
        }

        private void handleSetPlayerInventory(PacketSendEvent event, Player player) {
            WrapperPlayServerSetPlayerInventory wrapper = new WrapperPlayServerSetPlayerInventory(event);
            com.github.retrooper.packetevents.protocol.item.ItemStack replacement = resolveClientItem(
                    player,
                    wrapper.getStack(),
                    PlayerViewItemEvent.Source.SET_PLAYER_INVENTORY,
                    -1,
                    wrapper.getSlot(),
                    -1
            );

            if (replacement == null) return;
            wrapper.setStack(replacement);
            event.markForReEncode(true);
        }

        private com.github.retrooper.packetevents.protocol.item.ItemStack resolveClientItem(
                Player player,
                com.github.retrooper.packetevents.protocol.item.ItemStack packetItem,
                PlayerViewItemEvent.Source source,
                int windowId,
                int slot,
                int stateId
        ) {
            return cache.resolve(
                    player.getUniqueId(),
                    source,
                    windowId,
                    slot,
                    packetItem,
                    item -> callEvent(player, item, source, windowId, slot, stateId)
            );
        }

        private com.github.retrooper.packetevents.protocol.item.ItemStack callEvent(
                Player player,
                com.github.retrooper.packetevents.protocol.item.ItemStack packetItem,
                PlayerViewItemEvent.Source source,
                int windowId,
                int slot,
                int stateId
        ) {
            if (packetItem == null || packetItem.isEmpty()) return null;

            ItemStack bukkitItem = SpigotConversionUtil.toBukkitItemStack(packetItem);
            if (isEmpty(bukkitItem)) return null;

            PlayerViewItemEvent event = new PlayerViewItemEvent(
                    player,
                    bukkitItem,
                    source,
                    windowId,
                    slot,
                    stateId,
                    !Bukkit.isPrimaryThread()
            );
            plugin.getServer().getPluginManager().callEvent(event);

            ItemStack replacement = event.getItemStack();
            if (Objects.equals(bukkitItem, replacement)) return null;
            if (isEmpty(replacement)) return com.github.retrooper.packetevents.protocol.item.ItemStack.EMPTY;
            return SpigotConversionUtil.fromBukkitItemStack(replacement);
        }

        private static boolean isEmpty(ItemStack itemStack) {
            return itemStack == null || itemStack.getType() == Material.AIR || itemStack.getAmount() <= 0;
        }
    }

}
