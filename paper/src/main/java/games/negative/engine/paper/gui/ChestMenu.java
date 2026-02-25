package games.negative.engine.paper.gui;

import com.google.common.base.Preconditions;
import games.negative.engine.message.util.MiniMessageUtil;
import games.negative.engine.paper.gui.button.Button;
import games.negative.engine.paper.gui.util.SafeUtil;
import games.negative.engine.paper.scheduler.Scheduler;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Contract;

import java.util.*;

/**
 * Represents a chest-based GUI menu for players.
 * This class manages button placement, click handling, and automatic refreshing
 * of dynamic buttons. Menus are cached per-player and automatically cleaned up
 * when closed.
 */
public abstract class ChestMenu implements ChestInterface {

    private Component title = Component.text("Chest Menu");
    private int rows = 1;

    private final Map<Integer, Button> buttons = new HashMap<>();
    private final Map<UUID, Button> buttonById = new HashMap<>();
    private final Map<Integer, Button> refreshingButtons = new HashMap<>();

    protected InventoryView inventory;
    private boolean cancelClicks = true;

    private final Player player;

    /**
     * Creates a new ChestMenu.
     * @param player the player
     * @param title the title
     * @param rows the rows
     */

    public ChestMenu(Player player, String title, int rows) {
        setTitle(title);
        setRows(rows);
        this.player = player;
    }

    /**
     * Creates a new ChestMenu.
     * @param player the player
     */

    public ChestMenu(Player player) {
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
     * Set the number of rows in the chest menu
     * @param rows The number of rows to set
     */
    protected void setRows(int rows) {
        Preconditions.checkArgument(
                rows >= MIN_ROWS && rows <= MAX_ROWS,
                "The number of rows must be between 1 and 6 (inclusive)."
        );

        this.rows = rows;
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
        UserInterface.invalidateFromCache(player.getUniqueId());
    }

    /**
     * Executes onClick.
     * @param player the player
     * @param event the event
     */

    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        if (cancelClicks) {
            event.setCancelled(true);
            event.setResult(Event.Result.DENY);
        }

        ItemStack current = event.getCurrentItem();
        if (current == null) return;

        String id = current.getPersistentDataContainer().get(Button.KEY, PersistentDataType.STRING);
        if (id == null) return;

        Button button = buttonById.get(UUID.fromString(id));
        if (button == null) return;

        button.processClickAction(player, event);
    }

    /**
     * Open the chest menu for the player.
     */
    @Override
    public void open() {
        refresh();

        UserInterface.invalidateFromCache(player.getUniqueId());

        CACHE.put(player.getUniqueId(), this);

        Scheduler.entity(player).execute(() -> inventory.open(), 1);
    }

    /**
     * Executes invalidate.
     */

    @Override
    public void invalidate() {
        inventory = null;
        clearButtons();
    }

    /**
     * Refresh the chest menu, updating all buttons and their items.
     */
    @Override
    public void refresh() {
        if (inventory == null)
            inventory = typeFromRows(rows).create(player, title);

        clearInventory(inventory);

        buttons.forEach((slot, button) -> {
            button.onAddToInventory(null);

            ItemStack stack = button.item(player);
            if (stack == null || stack.getType().isAir()) return;

            stack.editPersistentDataContainer(
                    data -> data.set(Button.KEY, PersistentDataType.STRING, button.uuid().toString())
            );

            SafeUtil.setInventoryItem(inventory, slot, stack);
            button.onAddToInventory(inventory);
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
        if (inventory == null) return;

        Player player = (Player) inventory.getPlayer();

        ItemStack stack = button.item(player);
        if (stack == null || stack.getType().isAir()) return;

        stack.editPersistentDataContainer(
                data -> data.set(Button.KEY, PersistentDataType.STRING, button.uuid().toString())
        );

        SafeUtil.setInventoryItem(inventory, slot, stack);
    }

    /**
     * Add a button to a specific slot in the chest menu
     * @param slot The slot to add the button to
     * @param button The button to add
     */
    public void addButton(int slot, Button button) {
        Preconditions.checkArgument(
                slot >= 0 && slot < rows * 9,
                "Slot must be between 0 and " + (rows * 9 - 1)
        );

        buttons.put(slot, button);
        buttonById.put(button.uuid(), button);

        if (button.refreshIntervalTicks() <= 0L) return;

        refreshingButtons.put(slot, button);
    }

    /**
     * Add a button to the first available slot in the chest menu
     * @param button The button to add
     */
    public void addButton(Button button) {
        for (int i = 0; i < rows * 9; i++) {
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
     * Get the InventoryView associated with this ChestMenu
     * @return The InventoryView
     */
    @Override
    public InventoryView getView() {
        return inventory;
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
        ItemStack item = inventory.getItem(slot);
        if (item == null || item.getType().isAir()) return false;

        String id = item.getPersistentDataContainer().get(Button.KEY, PersistentDataType.STRING);
        if (id == null) return false;

        Button button = buttonById.get(UUID.fromString(id));
        if (button == null) return false;

        return button.cancelClick();
    }

}
