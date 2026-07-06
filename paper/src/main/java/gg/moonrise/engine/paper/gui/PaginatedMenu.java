package gg.moonrise.engine.paper.gui;

import com.google.common.base.Preconditions;
import gg.moonrise.engine.message.util.MiniMessageUtil;
import gg.moonrise.engine.paper.gui.button.Button;
import gg.moonrise.engine.paper.gui.holder.PaginatedMenuHolder;
import gg.moonrise.engine.paper.gui.layout.ContentSlotOrder;
import gg.moonrise.engine.paper.gui.layout.MenuLayout;
import gg.moonrise.engine.paper.gui.util.MenuInteractionUtil;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 * Represents a chest-based GUI menu for players.
 * This class manages button placement, click handling, and automatic refreshing
 * of dynamic buttons. Menus are routed through their inventory holder.
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

    protected Inventory inventory;
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
     * Set content slots from a layout key in horizontal order.
     * @param layout The layout to read
     * @param key The content key
     */
    public void setContentSlots(MenuLayout layout, char key) {
        setContentSlots(layout, key, ContentSlotOrder.HORIZONTAL);
    }

    /**
     * Set content slots from a layout key in the requested order.
     * @param layout The layout to read
     * @param key The content key
     * @param order The content slot order
     */
    public void setContentSlots(MenuLayout layout, char key, ContentSlotOrder order) {
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(order, "order");
        setContentSlots(layout.slots(key, order));
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
        if (nextPageButton != null) buttonById.remove(nextPageButton.getValue().uuid());
        nextPageButton = Map.entry(slot, button);
        buttonById.put(button.uuid(), button);
    }

    /**
     * Set the next page button using the first slot matching a layout key.
     * @param layout The layout to read
     * @param key The layout key
     * @param button The button to set
     */
    public void setNextPageButton(MenuLayout layout, char key, Button button) {
        setNextPageButton(layout.firstSlot(key), button);
    }

    /**
     * Set the previous page button for the paginated menu
     * @param slot The slot to place the button in
     * @param button The button to set
     */
    protected void setPreviousPageButton(int slot, Button button) {
        if (previousPageButton != null) buttonById.remove(previousPageButton.getValue().uuid());
        previousPageButton = Map.entry(slot, button);
        buttonById.put(button.uuid(), button);
    }

    /**
     * Set the previous page button using the first slot matching a layout key.
     * @param layout The layout to read
     * @param key The layout key
     * @param button The button to set
     */
    public void setPreviousPageButton(MenuLayout layout, char key, Button button) {
        setPreviousPageButton(layout.firstSlot(key), button);
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
     * This method invalidates the menu to free up resources.
     * @param player The player who closed the inventory.
     * @param event The InventoryCloseEvent triggered by the player closing the inventory.
     */
    @Override
    public void onClose(Player player, InventoryCloseEvent event) {
        invalidate();
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
     * This method refreshes the menu and opens the inventory for the player.
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
            inventory = createInventory();

        clearInventory(inventory);

        buttons.forEach((slot, button) -> {
            button.onAddToInventory(null);
            renderButtonToSlot(slot, button);
        });

        List<Button> items = pages.get(page - 1);
        if (items == null) items = Collections.emptyList();

        boolean hasNextPage = page < pages.size();
        boolean hasPreviousPage = page > 1;

        List<Integer> listingSlots = new ArrayList<>(contentSlots);
        listingSlots.removeAll(buttons.keySet());

        if (hasNextPage) {
            Preconditions.checkNotNull(nextPageButton, "Next Page Button cannot be null.");
            listingSlots.remove(Integer.valueOf(nextPageButton.getKey()));
        }

        if (hasPreviousPage) {
            Preconditions.checkNotNull(previousPageButton, "Previous Page Button cannot be null.");
            listingSlots.remove(Integer.valueOf(previousPageButton.getKey()));
        }

        refreshingContentButtons.clear();

        for (Button button : items) {
            int available = listingSlots.isEmpty() ? -1 : listingSlots.getFirst();
            if (available == -1) break;

            listingSlots.remove(Integer.valueOf(available));

            refreshButton(available, button);

            if (button.refreshIntervalTicks() > 0L) {
                refreshingContentButtons.put(available, button);
            }
        }

        if (hasNextPage) {
            renderButtonToSlot(nextPageButton.getKey(), nextPageButton.getValue());
        }

        if (hasPreviousPage) {
            renderButtonToSlot(previousPageButton.getKey(), previousPageButton.getValue());
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
        MenuInteractionUtil.refreshButton(inventory, slot, button, player);
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
        setContent(buttons, true);
    }

    /**
     * Set content without eagerly rendering every button to filter empty items.
     * This is preferred for large or dynamic content lists.
     * @param buttons The collection of buttons to set as content
     */
    public void setContentUnfiltered(Collection<Button> buttons) {
        setContent(buttons, false);
    }

    /**
     * Set the content of the paginated menu using a collection of buttons.
     * @param buttons The collection of buttons to set as content
     * @param filterEmptyItems true to preserve legacy eager empty-item filtering
     */
    public void setContent(Collection<Button> buttons, boolean filterEmptyItems) {
        List<Integer> listingSlots = new ArrayList<>(contentSlots);
        int limit = listingSlots.size();

        pages.clear();
        if (limit <= 0) return;

        List<Button> items = new ArrayList<>();
        for (Button button : buttons) {
            if (button == null) continue;
            if (filterEmptyItems) {
                ItemStack itemStack = button.item(player);
                if (itemStack == null || itemStack.getType().isAir()) continue;
            }
            items.add(button);
        }

        int pageIndex = 0;
        for (int i = 0; i < items.size(); i += limit) {
            List<Button> pageItems = new ArrayList<>(items.subList(i, Math.min(i + limit, items.size())));
            pages.put(pageIndex++, pageItems);
        }

        buttons.stream()
                .filter(Objects::nonNull)
                .forEach(button -> buttonById.put(button.uuid(), button));
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
     * Move to the next page if one exists.
     */
    public void nextPage() {
        changePage(page + 1);
    }

    /**
     * Move to the previous page if one exists.
     */
    public void previousPage() {
        changePage(page - 1);
    }

    /**
     * Get the current page number.
     * @return the current one-based page number
     */
    @Contract(pure = true)
    public int getPage() {
        return page;
    }

    /**
     * Get the total page count.
     * @return the page count
     */
    @Contract(pure = true)
    public int getPageCount() {
        return Math.max(1, pages.size());
    }

    /**
     * Check whether another page exists after the current page.
     * @return true if a next page exists
     */
    @Contract(pure = true)
    public boolean hasNextPage() {
        return page < pages.size();
    }

    /**
     * Check whether another page exists before the current page.
     * @return true if a previous page exists
     */
    @Contract(pure = true)
    public boolean hasPreviousPage() {
        return page > 1;
    }

    /**
     * Get the Inventory of this menu
     * @return The Inventory
     */
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Get the InventoryView of this menu
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

    /**
     * Render a button to a specific slot in the inventory
     * @param slot The slot to render the button to
     * @param button The button to render
     */
    private void renderButtonToSlot(int slot, Button button) {
        buttonById.put(button.uuid(), button);
        MenuInteractionUtil.renderButton(inventory, slot, button, player);
    }

    private Inventory createInventory() {
        PaginatedMenuHolder holder = new PaginatedMenuHolder(this);
        Inventory created = Bukkit.createInventory(holder, rows * 9, title);
        holder.setInventory(created);
        return created;
    }

}
