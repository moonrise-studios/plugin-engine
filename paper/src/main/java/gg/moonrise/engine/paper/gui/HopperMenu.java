package gg.moonrise.engine.paper.gui;

import gg.moonrise.engine.message.util.MiniMessageUtil;
import gg.moonrise.engine.paper.cooldown.Cooldowns;
import gg.moonrise.engine.paper.gui.button.Button;
import gg.moonrise.engine.paper.gui.holder.HopperMenuHolder;
import gg.moonrise.engine.paper.gui.layout.MenuLayout;
import gg.moonrise.engine.paper.gui.util.MenuInteractionUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.Contract;

import java.time.Duration;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Represents a chest-based GUI menu for players.
 * This class manages button placement, click handling, and automatic refreshing
 * of dynamic buttons. Menus are routed through their inventory holder.
 */
public abstract class HopperMenu implements UserInterface {

    private Component title = Component.text("Hopper Menu");

    private final Map<Integer, Button> buttons = new HashMap<>();
    private final Map<UUID, Button> buttonById = new HashMap<>();
    private final Map<Integer, Button> refreshingButtons = new HashMap<>();
    private final String interactionCooldownKey = "menu-interaction:" + UUID.randomUUID();
    private Duration interactionCooldown = DEFAULT_INTERACTION_COOLDOWN;

    protected Inventory inventory;
    private boolean cancelClicks = true;

    private final Player player;

    /**
     * Creates a new HopperMenu.
     * @param player the player
     * @param title the title
     */

    public HopperMenu(Player player, String title) {
        setTitle(title);
        this.player = player;
    }

    /**
     * Creates a new HopperMenu.
     * @param player the player
     */

    public HopperMenu(Player player) {
        this.player = player;
    }

    /**
     * Set the title of the chest menu
     * @param title The title to set
     */
    protected void setTitle(String title) {
        this.title = MiniMessageUtil.fromText(title);
    }

    /**
     * Sets the minimum delay between handled button interactions.
     *
     * @param duration the non-negative interaction cooldown
     */
    protected void setInteractionCooldown(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Interaction cooldown cannot be negative.");
        }
        Cooldowns.removeCooldown(player.getUniqueId(), interactionCooldownKey);
        interactionCooldown = duration;
    }

    /**
     * Handle the event when a player opens the inventory.
     * @param player The player who opened the inventory.
     * @param event The InventoryOpenEvent triggered by the player opening the inventory.
     */
    @Override
    public void onOpen(Player player, InventoryOpenEvent event) {

    }

    /**
     * Handle the event when a player closes the inventory.
     * @param player The player who closed the inventory.
     * @param event The InventoryCloseEvent triggered by the player closing the inventory.
     */
    @Override
    public void onClose(Player player, InventoryCloseEvent event) {
        invalidate();
    }

    /**
     * Executes onClick.
     * @param player the player
     * @param event the event
     */

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        MenuInteractionUtil.processClick(
                cancelClicks, buttonById, player, event, interactionCooldown, interactionCooldownKey
        );
    }

    /**
     * Open the chest menu for the player.
     */
    @Override
    public void open() {
        MenuInteractionUtil.openMenu(player, this::refresh, () -> inventory);
    }

    /**
     * Executes invalidate.
     */

    @Override
    public void invalidate() {
        Cooldowns.removeCooldown(player.getUniqueId(), interactionCooldownKey);
        inventory = null;
        clearButtons();
    }

    /**
     * Refresh the chest menu, updating all buttons and their items.
     */
    @Override
    public void refresh() {
        if (inventory == null)
            inventory = createInventory();

        clearInventory(inventory);

        buttons.forEach((slot, button) -> {
            button.onAddToInventory(null);
            MenuInteractionUtil.renderButton(inventory, slot, button, player);
        });
    }

    /**
     * Get all buttons that need to be refreshed periodically
     * @return A collection of buttons that need to be refreshed
     */
    public Collection<Map.Entry<Integer, Button>> getRefreshingButtons() {
        return new HashSet<>(refreshingButtons.entrySet());
    }

    /**
     * Refresh a specific button in the chest menu
     * @param slot Slot of the button to refresh
     * @param button The button to refresh
     */
    public void refreshButton(int slot, Button button) {
        MenuInteractionUtil.refreshButton(inventory, slot, button, player);
    }

    /**
     * Add a button to a specific slot in the chest menu
     * @param slot The slot to add the button to
     * @param button The button to add
     */
    public void addButton(int slot, Button button) {
        MenuInteractionUtil.addButton(slot, button, buttons, buttonById, refreshingButtons);
    }

    /**
     * Add a button to the first slot matching a layout key.
     * @param layout The layout to read
     * @param key The layout key
     * @param button The button to add
     */
    public void addButton(MenuLayout layout, char key, Button button) {
        addButton(layout.firstSlot(key), button);
    }

    /**
     * Fill every slot matching a layout key with newly-created buttons.
     * @param layout The layout to read
     * @param key The layout key
     * @param buttonSupplier The button supplier
     */
    public void addButtons(MenuLayout layout, char key, Supplier<Button> buttonSupplier) {
        Objects.requireNonNull(buttonSupplier, "buttonSupplier");
        addButtons(layout, key, slot -> buttonSupplier.get());
    }

    /**
     * Fill every slot matching a layout key with newly-created buttons.
     * @param layout The layout to read
     * @param key The layout key
     * @param buttonFactory The button factory
     */
    public void addButtons(MenuLayout layout, char key, IntFunction<Button> buttonFactory) {
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(buttonFactory, "buttonFactory");

        for (int slot : layout.slots(key)) {
            Button button = Objects.requireNonNull(buttonFactory.apply(slot), "buttonFactory returned null");
            addButton(slot, button);
        }
    }

    /**
     * Add a button to the first available slot in the chest menu
     * @param button The button to add
     */
    public void addButton(Button button) {
        for (int i = 0; i < InventoryType.HOPPER.getDefaultSize(); i++) {
            if (buttons.containsKey(i)) continue;

            addButton(i, button);
            break;
        }
    }

    /**
     * Remove a button from a specific slot
     * @param slot The slot to remove the button from
     * @return The removed button, or null if none existed
     */
    public Button removeButton(int slot) {
        Button removed = buttons.remove(slot);
        if (removed != null) {
            buttonById.remove(removed.uuid());
            refreshingButtons.remove(slot);
            removed.onAddToInventory(null);
        }
        return removed;
    }

    /**
     * Get the button at a specific slot
     * @param slot The slot to check
     * @return The button, or null if none exists
     */
    @Contract(pure = true)
    public Button getButton(int slot) {
        return buttons.get(slot);
    }

    /**
     * Check if a button exists at a specific slot
     * @param slot The slot to check
     * @return true if a button exists at the slot
     */
    @Contract(pure = true)
    public boolean hasButton(int slot) {
        return buttons.containsKey(slot);
    }

    /**
     * Clear all buttons from this menu
     */
    public void clearButtons() {
        buttons.values().forEach(button -> button.onAddToInventory(null));
        buttons.clear();
        buttonById.clear();
        refreshingButtons.clear();
    }

    /**
     * Get the total number of buttons in this menu
     * @return The number of buttons
     */
    @Contract(pure = true)
    public int getButtonCount() {
        return buttons.size();
    }

    /**
     * Get the Inventory associated with this menu
     * @return The Inventory
     */
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Get the InventoryView associated with this menu
     * @return The InventoryView
     */
    @Override
    @Deprecated(forRemoval = false)
    public InventoryView getView() {
        if (inventory == null) return null;

        InventoryView view = player.getOpenInventory();
        if (!view.getTopInventory().equals(inventory)) return null;

        return view;
    }

    /**
     * Set whether clicks in this inventory should be cancelled
     * @param cancelClicks Whether clicks should be cancelled
     */
    public void cancelClicks(boolean cancelClicks) {
        this.cancelClicks = cancelClicks;
    }

    /**
     * Check whether clicks in this inventory are cancelled
     * @return true if clicks are cancelled
     */
    public boolean cancelClicks() {
        return cancelClicks;
    }

    /**
     * Check if a click in a specific slot should be cancelled
     * @param slot The slot that was clicked
     * @return true if the click should be cancelled
     */
    public boolean checkCancelClick(int slot) {
        return MenuInteractionUtil.checkCancelClick(inventory, buttonById, slot);
    }

    private Inventory createInventory() {
        HopperMenuHolder holder = new HopperMenuHolder(this);
        Inventory created = Bukkit.createInventory(holder, InventoryType.HOPPER, title);
        holder.setInventory(created);
        return created;
    }

}
