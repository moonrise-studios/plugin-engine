package gg.moonrise.engine.paper.gui;

import com.google.common.base.Preconditions;
import gg.moonrise.engine.message.util.MiniMessageUtil;
import gg.moonrise.engine.paper.gui.button.Button;
import gg.moonrise.engine.paper.gui.holder.ScrollingMenuHolder;
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

/**
 * Represents a chest-based scrolling menu for players.
 * This class renders a fixed viewport over a larger list of content buttons.
 */
@Slf4j
public abstract class ScrollingMenu implements ChestInterface {

    private Component title = Component.text("Scrolling Menu");
    private int rows = 1;
    protected int line = 0;

    private final Player player;

    private Map.Entry<Integer, Button> nextLineButton;
    private Map.Entry<Integer, Button> previousLineButton;

    private final Map<Integer, Button> buttons = new HashMap<>();
    private final Map<UUID, Button> buttonById = new HashMap<>();
    private final Map<Integer, Button> refreshingButtons = new HashMap<>();

    private final List<Integer> contentSlots = new ArrayList<>();
    private final List<Button> content = new ArrayList<>();
    private final Map<Integer, Button> refreshingContentButtons = new HashMap<>();

    private int lineLength = -1;

    protected Inventory inventory;
    private boolean cancelClicks = true;

    /**
     * Creates a new ScrollingMenu.
     * @param player the player
     * @param title the title
     * @param rows the rows
     */
    public ScrollingMenu(Player player, String title, int rows) {
        setTitle(title);
        setRows(rows);
        this.player = player;
    }

    /**
     * Creates a new ScrollingMenu.
     * @param player the player
     */
    public ScrollingMenu(Player player) {
        this.player = player;
    }

    /**
     * Set the title of the scrolling menu
     * @param title The title to set
     */
    protected void setTitle(String title) {
        this.title = MiniMessageUtil.fromText(title);
    }

    /**
     * Set the number of rows in the scrolling menu
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
     * Set the content slots used as the visible scrolling viewport.
     * Slots are rendered in the given order.
     * @param slots The list of slots to set as content slots
     */
    protected void setContentSlots(List<Integer> slots) {
        contentSlots.clear();
        for (Integer slot : slots) {
            checkSlot(slot);
            if (contentSlots.contains(slot)) continue;
            contentSlots.add(slot);
        }
    }

    /**
     * Set the content slots from a list of strings.
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
     * Set how many content buttons make up one scroll line.
     * If unset, the first content row width is inferred from the configured slots.
     * @param lineLength The number of content buttons in one scroll line
     */
    protected void setLineLength(int lineLength) {
        Preconditions.checkArgument(lineLength > 0, "Line length must be greater than 0.");
        this.lineLength = lineLength;
    }

    /**
     * Set the next line button for the scrolling menu.
     * @param slot The slot to place the button in
     * @param button The button to set
     */
    protected void setNextLineButton(int slot, Button button) {
        checkSlot(slot);
        if (nextLineButton != null) {
            nextLineButton.getValue().onAddToInventory(null);
            buttonById.remove(nextLineButton.getValue().uuid());
        }
        nextLineButton = Map.entry(slot, button);
        buttonById.put(button.uuid(), button);
    }

    /**
     * Set the previous line button for the scrolling menu.
     * @param slot The slot to place the button in
     * @param button The button to set
     */
    protected void setPreviousLineButton(int slot, Button button) {
        checkSlot(slot);
        if (previousLineButton != null) {
            previousLineButton.getValue().onAddToInventory(null);
            buttonById.remove(previousLineButton.getValue().uuid());
        }
        previousLineButton = Map.entry(slot, button);
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
     * @param player The player who closed the inventory.
     * @param event The InventoryCloseEvent triggered by the player closing the inventory.
     */
    @Override
    public void onClose(Player player, InventoryCloseEvent event) {
        invalidate();
    }

    /**
     * Handle the event when a player clicks within the inventory.
     * @param player The player who clicked in the inventory.
     * @param event The InventoryClickEvent triggered by the player's click.
     */
    @Override
    public void onClick(Player player, InventoryClickEvent event) {
        MenuInteractionUtil.processClick(cancelClicks, buttonById, player, event);
    }

    /**
     * Open the scrolling menu for the player.
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
     * Refresh the scrolling menu by updating fixed buttons, content, and scroll controls.
     */
    @Override
    public void refresh() {
        if (inventory == null)
            inventory = createInventory();

        clearInventory(inventory);
        content.forEach(button -> button.onAddToInventory(null));
        if (nextLineButton != null) nextLineButton.getValue().onAddToInventory(null);
        if (previousLineButton != null) previousLineButton.getValue().onAddToInventory(null);

        buttons.forEach((slot, button) -> {
            button.onAddToInventory(null);
            renderButtonToSlot(slot, button);
        });

        List<Integer> viewportSlots = viewportSlots();
        int lineLength = effectiveLineLength(viewportSlots);
        int maxLine = maxLine(viewportSlots, lineLength);
        line = clampLine(line, maxLine);

        boolean hasNextLine = line < maxLine;
        boolean hasPreviousLine = line > 0;

        refreshingContentButtons.clear();

        int contentIndex = line * lineLength;
        for (Integer slot : viewportSlots) {
            if (contentIndex >= content.size()) break;

            Button button = content.get(contentIndex++);
            renderButtonToSlot(slot, button);

            if (button.refreshIntervalTicks() > 0L) {
                refreshingContentButtons.put(slot, button);
            }
        }

        if (hasNextLine) {
            Preconditions.checkNotNull(nextLineButton, "Next Line Button cannot be null.");
            renderButtonToSlot(nextLineButton.getKey(), nextLineButton.getValue());
        }

        if (hasPreviousLine) {
            Preconditions.checkNotNull(previousLineButton, "Previous Line Button cannot be null.");
            renderButtonToSlot(previousLineButton.getKey(), previousLineButton.getValue());
        }
    }

    /**
     * Get all buttons that need to be refreshed periodically.
     * @return A collection of buttons that need to be refreshed
     */
    public Collection<Map.Entry<Integer, Button>> getRefreshingButtons() {
        return new HashSet<>(refreshingButtons.entrySet());
    }

    /**
     * Get all visible content buttons that need to be refreshed periodically.
     * @return A collection of content buttons that need to be refreshed
     */
    public Collection<Map.Entry<Integer, Button>> getRefreshingContentButtons() {
        return new HashSet<>(refreshingContentButtons.entrySet());
    }

    /**
     * Refresh a specific button in the scrolling menu.
     * @param slot Slot of the button to refresh
     * @param button The button to refresh
     */
    public void refreshButton(int slot, Button button) {
        MenuInteractionUtil.refreshButton(inventory, slot, button, player);
    }

    /**
     * Add a button to a specific slot in the scrolling menu.
     * @param slot The slot to add the button to
     * @param button The button to add
     */
    public void addButton(int slot, Button button) {
        checkSlot(slot);

        MenuInteractionUtil.addButton(slot, button, buttons, buttonById, refreshingButtons);
    }

    /**
     * Add a button to the first available slot in the scrolling menu.
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
     * Remove a button from a specific slot.
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
     * Get the button at a specific slot.
     * @param slot The slot to check
     * @return The button, or null if none exists
     */
    @Contract(pure = true)
    public Button getButton(int slot) {
        return buttons.get(slot);
    }

    /**
     * Check if a button exists at a specific slot.
     * @param slot The slot to check
     * @return true if a button exists at the slot
     */
    @Contract(pure = true)
    public boolean hasButton(int slot) {
        return buttons.containsKey(slot);
    }

    /**
     * Clear all fixed button registrations from this menu.
     */
    public void clearButtons() {
        buttons.values().forEach(button -> button.onAddToInventory(null));
        content.forEach(button -> button.onAddToInventory(null));
        if (nextLineButton != null) nextLineButton.getValue().onAddToInventory(null);
        if (previousLineButton != null) previousLineButton.getValue().onAddToInventory(null);
        buttons.clear();
        buttonById.clear();
        refreshingButtons.clear();
        refreshingContentButtons.clear();
    }

    /**
     * Get the total number of fixed buttons in this menu.
     * @return The number of fixed buttons
     */
    @Contract(pure = true)
    public int getButtonCount() {
        return buttons.size();
    }

    /**
     * Generate buttons from a collection using a button function.
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
     * Set the content of the scrolling menu using a collection of buttons.
     * @param buttons The collection of buttons to set as content
     */
    public void setContent(Collection<Button> buttons) {
        content.forEach(button -> {
            button.onAddToInventory(null);
            buttonById.remove(button.uuid());
        });

        content.clear();
        refreshingContentButtons.clear();

        for (Button button : buttons) {
            ItemStack itemStack = button.item(player);
            if (itemStack == null || itemStack.getType().isAir()) continue;
            content.add(button);
            buttonById.put(button.uuid(), button);
        }

        line = clampLine(line, maxLine(viewportSlots(), effectiveLineLength(viewportSlots())));
    }

    /**
     * Change the current scroll line.
     * @param line The line number to change to
     */
    public void changeLine(int line) {
        this.line = clampLine(line, maxLine(viewportSlots(), effectiveLineLength(viewportSlots())));
        refresh();
    }

    /**
     * Scroll up by one line.
     */
    public void previousLine() {
        changeLine(line - 1);
    }

    /**
     * Scroll down by one line.
     */
    public void nextLine() {
        changeLine(line + 1);
    }

    /**
     * Get the current scroll line.
     * @return The current scroll line
     */
    @Contract(pure = true)
    public int getLine() {
        return line;
    }

    /**
     * Get the maximum scroll line for the current content and viewport.
     * @return The maximum scroll line
     */
    @Contract(pure = true)
    public int getMaxLine() {
        List<Integer> viewportSlots = viewportSlots();
        return maxLine(viewportSlots, effectiveLineLength(viewportSlots));
    }

    /**
     * Get the Inventory of this menu.
     * @return The Inventory
     */
    @Override
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Get the InventoryView of this menu.
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
     * Set whether clicks in this inventory should be cancelled.
     * @param cancelClicks Whether clicks should be cancelled
     */
    public void cancelClicks(boolean cancelClicks) {
        this.cancelClicks = cancelClicks;
    }

    /**
     * Check whether clicks in this inventory are cancelled.
     * @return true if clicks are cancelled
     */
    public boolean cancelClicks() {
        return cancelClicks;
    }

    /**
     * Check if a click in a specific slot should be cancelled.
     * @param slot The slot that was clicked
     * @return true if the click should be cancelled
     */
    public boolean checkCancelClick(int slot) {
        return MenuInteractionUtil.checkCancelClick(inventory, buttonById, slot);
    }

    private void checkSlot(int slot) {
        Preconditions.checkArgument(
                slot >= 0 && slot < rows * 9,
                "Slot must be between 0 and " + (rows * 9 - 1)
        );
    }

    private void addRangeSlots(String rangeString) {
        try {
            String[] range = rangeString.split("-", 2);
            int start = Integer.parseInt(range[0].trim());
            int end = Integer.parseInt(range[1].trim());

            for (int i = start; i <= end; i++) {
                addContentSlot(i);
            }
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException exception) {
            log.warn("Invalid content slot range '{}'", rangeString, exception);
        }
    }

    private void addSingleSlot(String slotString) {
        try {
            addContentSlot(Integer.parseInt(slotString.trim()));
        } catch (NumberFormatException exception) {
            log.warn("Invalid content slot '{}'", slotString, exception);
        }
    }

    private void addContentSlot(int slot) {
        if (slot < 0 || slot >= rows * 9) {
            log.warn("Invalid content slot {} for menu size {}", slot, rows * 9);
            return;
        }

        if (!contentSlots.contains(slot)) {
            contentSlots.add(slot);
        }
    }

    private List<Integer> viewportSlots() {
        List<Integer> viewportSlots = new ArrayList<>(contentSlots);
        viewportSlots.removeAll(buttons.keySet());

        if (nextLineButton != null) {
            viewportSlots.remove(Integer.valueOf(nextLineButton.getKey()));
        }

        if (previousLineButton != null) {
            viewportSlots.remove(Integer.valueOf(previousLineButton.getKey()));
        }

        return viewportSlots;
    }

    private int effectiveLineLength(List<Integer> viewportSlots) {
        if (lineLength > 0) return lineLength;
        if (viewportSlots.isEmpty()) return 1;

        int firstRow = viewportSlots.getFirst() / 9;
        int inferred = 0;
        for (Integer slot : viewportSlots) {
            if (slot / 9 != firstRow) continue;
            inferred++;
        }

        return Math.max(1, inferred);
    }

    private int maxLine(List<Integer> viewportSlots, int lineLength) {
        if (viewportSlots.isEmpty() || content.isEmpty()) return 0;

        int visibleLines = (int) Math.ceil((double) viewportSlots.size() / lineLength);
        int contentLines = (int) Math.ceil((double) content.size() / lineLength);

        return Math.max(0, contentLines - visibleLines);
    }

    private int clampLine(int line, int maxLine) {
        return Math.max(0, Math.min(line, maxLine));
    }

    /**
     * Render a button to a specific slot in the inventory.
     * @param slot The slot to render the button to
     * @param button The button to render
     */
    private void renderButtonToSlot(int slot, Button button) {
        buttonById.put(button.uuid(), button);

        if (button.refreshIntervalTicks() > 0L && buttons.containsValue(button)) {
            refreshingButtons.put(slot, button);
        }

        MenuInteractionUtil.renderButton(inventory, slot, button, player);
    }

    private Inventory createInventory() {
        ScrollingMenuHolder holder = new ScrollingMenuHolder(this);
        Inventory created = Bukkit.createInventory(holder, rows * 9, title);
        holder.setInventory(created);
        return created;
    }

}
