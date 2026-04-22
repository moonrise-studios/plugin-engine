package gg.moonrise.engine.paper.gui.button;

import lombok.RequiredArgsConstructor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;

import java.util.UUID;
import java.util.function.Function;

/**
 * Represents the Button class.
 */

@RequiredArgsConstructor
public final class Button {

    public static final NamespacedKey KEY = new NamespacedKey("skylands-ui", "button-uuid");

    private final Function<Player, ItemStack> displayItem;
    private final ButtonClickAction clickAction;
    private final UUID uuid = UUID.randomUUID();
    private final long refreshIntervalTicks;
    private final boolean cancelClicks;
    private long lastRefreshTime = 0L;
    private InventoryView boundingInventory;


    /**
     * Executes onAddToInventory.
     * @param inventory the inventory
     */
    public void onAddToInventory(InventoryView inventory) {
        this.boundingInventory = inventory;
    }

    /**
     * Processes the click action for the button.
     * @param player the player who clicked the button
     * @param event the inventory click event
     */
    public void processClickAction(Player player, InventoryClickEvent event) {
        if (clickAction == null) return;

        clickAction.onClick(this, player, event);
    }

    /**
     * Gets the click action for the button.
     * @return the click action
     */
    public ButtonClickAction clickAction() {
        return clickAction;
    }

    /**
     * Gets the UUID of the button.
     * @return the UUID
     */
    public UUID uuid() {
        return uuid;
    }

    /**
     * Gets the display item for the button for a specific player.
     * @param player the player
     * @return the display item
     */
    public ItemStack item(Player player) {
        return displayItem.apply(player);
    }

    /**
     * Gets the inventory that this button is bound to.
     * @return the bounding inventory
     */
    public InventoryView boundingInventory() {
        return boundingInventory;
    }

    /**
     * Gets the refresh interval in ticks.
     * @return the refresh interval in ticks
     */
    public long refreshIntervalTicks() {
        return refreshIntervalTicks;
    }

    /**
     * Gets the last refresh time in ticks.
     * @return the last refresh time
     */
    public long lastRefreshTime() {
        return lastRefreshTime;
    }

    /**
     * Sets the last refresh time in ticks.
     * @param time the last refresh time
     */
    @Contract(mutates = "this")
    public void setLastRefreshTime(long time) {
        this.lastRefreshTime = time;
    }

    /**
     * Gets the builder for creating a Button.
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Executes cancelClick.
     * @return the result
     */

    public boolean cancelClick() {
        return cancelClicks;
    }

    /**
     * Represents the builder for creating a Button.
     */
    public static class Builder {

        private Function<Player, ItemStack> displayItem;
        private ButtonClickAction clickAction;
        private long refreshIntervalTicks = 0L;
        private boolean cancelClicks = true;

        /**
         * Sets the display item for the button.
         * @param displayItem the function to get the display item
         * @return the builder
         */
        public Builder item(Function<Player, ItemStack> displayItem) {
            this.displayItem = displayItem;
            return this;
        }

        /**
         * Sets the click action for the button.
         * @param clickAction the click action
         * @return the builder
         */
        public Builder action(ButtonClickAction clickAction) {
            this.clickAction = clickAction;
            return this;
        }

        /**
         * Sets the refresh interval for the button in ticks.
         * @param ticks the refresh interval in ticks
         * @return the builder
         */
        public Builder refresh(long ticks) {
            this.refreshIntervalTicks = ticks;
            return this;
        }

        /**
         * Executes cancelClick.
         * @param cancelClick the cancelClick
         * @return the result
         */

        public Builder cancelClick(boolean cancelClick) {
            this.cancelClicks = cancelClick;
            return this;
        }

        /**
         * Builds the button.
         * @return the button
         */
        public Button build() {
            if (displayItem == null) throw new IllegalStateException("Display item must be set");

            return new Button(
                    displayItem,
                    clickAction,
                    refreshIntervalTicks,
                    cancelClicks
            );
        }

    }

}
