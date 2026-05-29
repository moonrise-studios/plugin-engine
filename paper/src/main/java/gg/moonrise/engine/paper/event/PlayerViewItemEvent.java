package gg.moonrise.engine.paper.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when an item stack is about to be sent to a player's client.
 */
public class PlayerViewItemEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ItemStack originalItemStack;
    private final Source source;
    private final int windowId;
    private final int slot;
    private final int stateId;
    private ItemStack itemStack;

    public PlayerViewItemEvent(
            @NotNull Player player,
            @Nullable ItemStack itemStack,
            @NotNull Source source,
            int windowId,
            int slot,
            int stateId,
            boolean async
    ) {
        super(async);
        this.player = player;
        this.itemStack = cloneItem(itemStack);
        this.originalItemStack = cloneItem(itemStack);
        this.source = source;
        this.windowId = windowId;
        this.slot = slot;
        this.stateId = stateId;
    }

    public @NotNull Player getPlayer() {
        return player;
    }

    public @Nullable ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(@Nullable ItemStack itemStack) {
        this.itemStack = cloneItem(itemStack);
    }

    public @Nullable ItemStack getOriginalItemStack() {
        return cloneItem(originalItemStack);
    }

    public @NotNull Source getSource() {
        return source;
    }

    public int getWindowId() {
        return windowId;
    }

    public int getSlot() {
        return slot;
    }

    public int getStateId() {
        return stateId;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    private static ItemStack cloneItem(ItemStack itemStack) {
        return itemStack == null ? null : itemStack.clone();
    }

    public enum Source {
        SET_SLOT,
        WINDOW_ITEMS,
        SET_CURSOR_ITEM,
        SET_PLAYER_INVENTORY,
        WINDOW_CARRIED_ITEM
    }
}
