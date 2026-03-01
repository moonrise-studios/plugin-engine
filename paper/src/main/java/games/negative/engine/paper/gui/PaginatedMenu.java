package games.negative.engine.paper.gui;

import com.google.common.base.Preconditions;
import games.negative.engine.message.util.MiniMessageUtil;
import games.negative.engine.paper.gui.button.Button;
import games.negative.engine.paper.gui.util.MenuInteractionUtil;
import games.negative.engine.paper.gui.util.SafeUtil;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Contract;

import java.util.*;
import java.util.function.Function;

/**
 * Represents a chest-based GUI menu for players.
 * This class manages button placement, click handling, and automatic refreshing
 * of dynamic buttons. Menus are cached per-player and automatically cleaned up
 * when closed.
 */
@Slf4j
public abstract class PaginatedMenu implements ChestInterface {

    private Component title = Component.text("Paginated Menu");
    private int rows = 1;
    protected int page = 1;

    private final Player player;

    private Map.Entry<Integer, Button> nextPageButton;
    private Map.Entry<Integer, Button> previousPageButton;

    private final Map<Integer, Button> buttons = new HashMap<>();
    private final Map<UUID, Button> buttonById = new HashMap<>();
    private final Map<Integer, Button> refreshingButtons = new HashMap<>();

    private final Map<Integer, List<Button>> pages = new HashMap<>();
    private final List<Integer> contentSlots = new ArrayList<>();
    private final Map<Integer, Button> refreshingContentButtons = new HashMap<>();

    protected InventoryView inventory;
    private boolean cancelClicks = true;

    /**
     * Creates a new PaginatedMenu.
     * @param player the player
     * @param title the title
     * @param rows the rows
     */

    public PaginatedMenu(Player player, String title, int rows) {
        setTitle(title);
        setRows(rows);
        this.player = player;
    }

    /**
     * Creates a new PaginatedMenu.
     * @param player the player
     */

    public PaginatedMenu(Player player) {
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
     * Set the content slots for the paginated menu
     * @param slots The list of slots to set as content slots
     */
    protected void setContentSlots(List<Integer> slots) {
        contentSlots.clear();
        for (Integer slot : slots) {
            if (contentSlots.contains(slot)) continue;
            contentSlots.add(slot);
        }
    }

    /**
     * Set the content slots for the paginated menu from a list of strings
     * @param slotStrings The list of slot strings to set as content slots
     */
    protected void setContentSlotsFromString(List<String> slotStrings) {
        contentSlots.clear();
        slotStrings.forEach(slotString -> {
            if (slotString.contains("-")) {
                addRangeSlots(slotString);
            } else {
                addSingleSlot(slotString);
            }
        });
    }

    /**
     * Add a range of slots to the content slots
     * @param rangeString The range string to parse (e.g. "0-8")
     */
    private void addRangeSlots(String rangeString) {
        try {
            String[] range = rangeString.split("-", 2);
            int start = Integer.parseInt(range[0].trim());
            int end = Integer.parseInt(range[1].trim());

            for (int i = start; i <= end; i++) {
                if (contentSlots.contains(i)) continue;
                contentSlots.add(i);
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException exception) {
            log.warn("Invalid content slot range '{}'", rangeString, exception);
        }
    }

    /**
     * Add a single slot to the content slots
     * @param slotString The slot string to parse
     */
    private void addSingleSlot(String slotString) {
        try {
            int slot = Integer.parseInt(slotString.trim());
            if (!contentSlots.contains(slot)) {
                contentSlots.add(slot);
            }
        } catch (NumberFormatException exception) {
            log.warn("Invalid content slot '{}'", slotString, exception);
        }
    }

    /**
     * Set the next page button for the paginated menu
     * @param slot The slot to place the button in
     * @param button The button to set
     */
    protected void setNextPageButton(int slot, Button button) {
        nextPageButton = Map.entry(slot, button);
        buttonById.put(button.uuid(), button);
    }

    /**
     * Set the previous page button for the paginated menu
     * @param slot The slot to place the button in
     * @param button The button to set
     */
    protected void setPreviousPageButton(int slot, Button button) {
        previousPageButton = Map.entry(slot, button);
        buttonById.put(button.uuid(), button);
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
     * This method invalidates the menu from the cache to free up resources.
     * @param player The player who closed the inventory.
     * @param event The InventoryCloseEvent triggered by the player closing the inventory.
     */
    @Override
    public void onClose(Player player, InventoryCloseEvent event) {
        UserInterface.invalidateFromCache(player.getUniqueId());
    }

    /**
     * Handle the event when a player clicks within the inventory.
     * This method processes the click action associated with the clicked button.
     * @param player The player who clicked in the inventory.
     * @param event The InventoryClickEvent triggered by the player's click.
     */
    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        MenuInteractionUtil.processClick(false, buttonById, player, event);
    }

    /**
     * Open the chest menu for the player.
     * This method refreshes the menu, invalidates any existing cached menu for the player,
     * caches the current menu, and opens the inventory for the player.
     */
    @Override
    public void open() {
        MenuInteractionUtil.openMenu(player, this, this::refresh, () -> inventory);
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
     * Refresh the chest menu by updating all buttons and content.
     * This method recreates the inventory if necessary, clears existing items,
     * and re-renders all buttons and paginated content.
     */
    @Override
    public void refresh() {
        if (inventory == null)
            inventory = typeFromRows(rows).create(player, title);

        clearInventory(inventory);

        buttons.forEach((slot, button) -> {
            button.onAddToInventory(null);
            renderButtonToSlot(slot, button);
        });

        List<Integer> listingSlots = new ArrayList<>(contentSlots);
        int limit = listingSlots.size();

        List<Button> items = pages.get(page - 1);
        if (items == null) items = Collections.emptyList();

        if (items.size() >= limit) {
            Preconditions.checkNotNull(nextPageButton, "Next Page Button cannot be null.");

            int slot = nextPageButton.getKey();
            Button button = nextPageButton.getValue();

            renderButtonToSlot(slot, button);
        } else {
            if (nextPageButton != null) {
                int slot = nextPageButton.getKey();
                SafeUtil.setInventoryItem(inventory, slot, null);
            }
        }

        if (page > 1) {
            Preconditions.checkNotNull(previousPageButton, "Previous Page Button cannot be null.");

            int slot = previousPageButton.getKey();
            Button button = previousPageButton.getValue();

            renderButtonToSlot(slot, button);
        } else {
            if (previousPageButton != null) {
                int slot = previousPageButton.getKey();
                SafeUtil.setInventoryItem(inventory, slot, null);
            }
        }

        refreshingContentButtons.clear();

        for (Button button : items) {
            int available = listingSlots.isEmpty() ? -1 : listingSlots.getFirst();
            if (available == -1) break;

            listingSlots.remove(Integer.valueOf(available));

            refreshButton(available, button);

            refreshingContentButtons.put(available, button);
        }
    }

    /**
     * Get all buttons that need to be refreshed periodically
     * @return A collection of buttons that need to be refreshed
     */
    public Collection<Map.Entry<Integer, Button>> getRefreshingButtons() {
        return new HashSet<>(refreshingButtons.entrySet());
    }

    /**
     * Get all content buttons that need to be refreshed periodically
     * @return A collection of content buttons that need to be refreshed
     */
    public Collection<Map.Entry<Integer, Button>> getRefreshingContentButtons() {
        return new HashSet<>(refreshingContentButtons.entrySet());
    }

    /**
     * Refresh a specific button in the chest menu
     * @param slot Slot of the button to refresh
     * @param button The button to refresh
     */
    public void refreshButton(int slot, Button button) {
        renderButtonToSlot(slot, button);
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

        MenuInteractionUtil.addButton(slot, button, buttons, buttonById, refreshingButtons);
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
        refreshingContentButtons.clear();
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
     * Generate buttons from a collection using a button function
     * @param collection The collection of items to generate buttons from
     * @param buttonFunction The function to create buttons from items
     * @param <T> The type of the collection
     * @param <K> The type of items in the collection
     * @return A collection of generated buttons
     */
    public <T extends Iterable<K>, K> Collection<Button> generateButtons(T collection, Function<K, Button> buttonFunction) {
        Collection<Button> buttons = new ArrayList<>();
        for (K item : collection) {
            Button button = buttonFunction.apply(item);
            if (button == null) continue;

            buttons.add(button);
        }
        return buttons;
    }

    /**
     * Set the content of the paginated menu using a collection of buttons
     * @param buttons The collection of buttons to set as content
     */
    public void setContent(Collection<Button> buttons) {
        List<Integer> listingSlots = new ArrayList<>(contentSlots);
        int limit = listingSlots.size();

        pages.clear();
        if (limit <= 0) return;

        List<Button> items = new ArrayList<>();
        for (Button button : buttons) {
            ItemStack itemStack = button.item(player);
            if (itemStack == null || itemStack.getType().isAir()) continue;
            items.add(button);
        }

        int pageIndex = 0;
        for (int i = 0; i < items.size(); i += limit) {
            List<Button> pageItems = items.subList(i, Math.min(i + limit, items.size()));
            pages.put(pageIndex++, pageItems);
        }

        buttons.forEach(button -> buttonById.put(button.uuid(), button));
    }

    /**
     * Change the current page of the paginated menu
     * @param page The page number to change to
     */
    public void changePage(int page) {
        int maxPage = Math.max(1, pages.size());
        this.page = Math.max(1, Math.min(page, maxPage));
        refresh();
    }

    /**
     * Get the InventoryView of this menu
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
        return MenuInteractionUtil.checkCancelClick(inventory, buttonById, slot);
    }

    /**
     * Render a button to a specific slot in the inventory
     * @param slot The slot to render the button to
     * @param button The button to render
     */
    private void renderButtonToSlot(int slot, Button button) {
        ItemStack stack = button.item(player);
        if (stack == null || stack.getType().isAir()) return;

        stack.editPersistentDataContainer(
                data -> data.set(Button.KEY, PersistentDataType.STRING, button.uuid().toString())
        );

        SafeUtil.setInventoryItem(inventory, slot, stack);
        button.onAddToInventory(inventory);
    }

}
