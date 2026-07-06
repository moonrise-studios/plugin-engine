package gg.moonrise.engine.paper.gui;

import com.google.common.base.Preconditions;
import gg.moonrise.engine.message.util.MiniMessageUtil;
import gg.moonrise.engine.paper.gui.button.Button;
import gg.moonrise.engine.paper.gui.holder.StaticScrollingMenuHolder;
import gg.moonrise.engine.paper.gui.util.MenuInteractionUtil;
import gg.moonrise.engine.paper.gui.util.SafeUtil;
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

/**
 * Represents a chest-based static scrolling menu for players.
 * This class renders a scrolling viewport over layout lines while configured
 * static lines stay pinned in the visible inventory.
 */
public abstract class StaticScrollingMenu implements ChestInterface {

    private static final int COLUMNS = 9;

    private Component title = Component.text("Static Scrolling Menu");
    private int rows = 1;
    protected int line = 0;

    private final Player player;

    private final List<List<String>> layout = new ArrayList<>();
    private final Set<Integer> staticLines = new TreeSet<>();
    private final Map<String, Button> buttons = new HashMap<>();
    private final Map<UUID, Button> buttonById = new HashMap<>();
    private final Map<Integer, Button> refreshingButtons = new HashMap<>();
    private final Set<Button> renderedButtons = new HashSet<>();

    private Map.Entry<String, Button> nextLineButton;
    private Map.Entry<String, Button> previousLineButton;

    protected Inventory inventory;
    private boolean cancelClicks = true;

    /**
     * Creates a new StaticScrollingMenu.
     * @param player the player
     * @param title the title
     * @param rows the visible rows
     */
    public StaticScrollingMenu(Player player, String title, int rows) {
        setTitle(title);
        setRows(rows);
        this.player = player;
    }

    /**
     * Creates a new StaticScrollingMenu.
     * @param player the player
     */
    public StaticScrollingMenu(Player player) {
        this.player = player;
    }

    /**
     * Set the title of the static scrolling menu.
     * @param title The title to set
     */
    protected void setTitle(String title) {
        this.title = MiniMessageUtil.fromText(title);
    }

    /**
     * Set the number of visible rows in the static scrolling menu.
     * @param rows The number of rows to set
     */
    protected void setRows(int rows) {
        Preconditions.checkArgument(
                rows >= MIN_ROWS && rows <= MAX_ROWS,
                "The number of rows must be between 1 and 6 (inclusive)."
        );
        this.rows = rows;
        validateStaticLineCount();
        line = clampLine(line, getMaxLine());
    }

    /**
     * Set the full virtual layout for this menu.
     * Each line must contain exactly nine symbols, either space-separated or compact.
     * @param lines The virtual layout lines
     */
    protected void setLayout(List<String> lines) {
        Preconditions.checkArgument(lines != null && !lines.isEmpty(), "Layout must contain at least one line.");

        layout.clear();
        for (int i = 0; i < lines.size(); i++) {
            layout.add(parseLayoutLine(lines.get(i), i));
        }

        validateStaticLines();
        line = clampLine(line, getMaxLine());
    }

    /**
     * Set the layout lines that should stay pinned while the other lines scroll.
     * @param lines The static layout line indexes
     */
    protected void setStaticLines(Collection<Integer> lines) {
        Preconditions.checkArgument(lines != null, "Static lines cannot be null.");

        staticLines.clear();
        for (Integer staticLine : lines) {
            Preconditions.checkArgument(staticLine != null && staticLine >= 0, "Static line indexes must be non-negative.");
            staticLines.add(staticLine);
        }

        validateStaticLineCount();
        validateStaticLines();
        line = clampLine(line, getMaxLine());
    }

    /**
     * Set the layout lines that should stay pinned while the other lines scroll.
     * @param lines The static layout line indexes
     */
    protected void setStaticLines(Integer... lines) {
        setStaticLines(Arrays.asList(lines));
    }

    /**
     * Set the button rendered for a layout symbol.
     * @param symbol The layout symbol
     * @param button The button to render
     */
    protected void setButton(String symbol, Button button) {
        checkSymbol(symbol);
        Preconditions.checkArgument(button != null, "Button cannot be null.");

        Button replaced = buttons.put(symbol, button);
        if (replaced != null) {
            replaced.onAddToInventory(null);
            buttonById.remove(replaced.uuid());
        }
    }

    /**
     * Set the button rendered for a layout symbol.
     * @param symbol The layout symbol
     * @param button The button to render
     */
    protected void setButton(char symbol, Button button) {
        setButton(String.valueOf(symbol), button);
    }

    /**
     * Set the next-line button rendered for a layout symbol when a next line exists.
     * @param symbol The layout symbol
     * @param button The button to render
     */
    protected void setNextLineButton(String symbol, Button button) {
        checkSymbol(symbol);
        Preconditions.checkArgument(button != null, "Button cannot be null.");

        if (nextLineButton != null) {
            nextLineButton.getValue().onAddToInventory(null);
            buttonById.remove(nextLineButton.getValue().uuid());
        }
        nextLineButton = Map.entry(symbol, button);
    }

    /**
     * Set the next-line button rendered for a layout symbol when a next line exists.
     * @param symbol The layout symbol
     * @param button The button to render
     */
    protected void setNextLineButton(char symbol, Button button) {
        setNextLineButton(String.valueOf(symbol), button);
    }

    /**
     * Set the previous-line button rendered for a layout symbol when a previous line exists.
     * @param symbol The layout symbol
     * @param button The button to render
     */
    protected void setPreviousLineButton(String symbol, Button button) {
        checkSymbol(symbol);
        Preconditions.checkArgument(button != null, "Button cannot be null.");

        if (previousLineButton != null) {
            previousLineButton.getValue().onAddToInventory(null);
            buttonById.remove(previousLineButton.getValue().uuid());
        }
        previousLineButton = Map.entry(symbol, button);
    }

    /**
     * Set the previous-line button rendered for a layout symbol when a previous line exists.
     * @param symbol The layout symbol
     * @param button The button to render
     */
    protected void setPreviousLineButton(char symbol, Button button) {
        setPreviousLineButton(String.valueOf(symbol), button);
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
     * Open the static scrolling menu for the player.
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
     * Refresh the static scrolling menu by updating pinned and scrolling layout lines.
     */
    @Override
    public void refresh() {
        if (inventory == null)
            inventory = createInventory();

        validateStaticLines();
        clearInventory(inventory);
        detachRenderedButtons();
        buttonById.clear();
        refreshingButtons.clear();

        Map<Integer, Integer> staticLinesByDisplayRow = staticLinesByDisplayRow();
        List<Integer> scrollingLines = scrollingLines();
        int scrollRows = rows - staticLinesByDisplayRow.size();
        line = clampLine(line, maxLine(scrollingLines.size(), scrollRows));

        boolean hasNextLine = line < maxLine(scrollingLines.size(), scrollRows);
        boolean hasPreviousLine = line > 0;
        int scrollingLineOffset = line;

        for (int row = 0; row < rows; row++) {
            Integer staticLine = staticLinesByDisplayRow.get(row);
            if (staticLine != null) {
                renderLayoutLine(row, layout.get(staticLine), hasPreviousLine, hasNextLine);
                continue;
            }

            if (scrollingLineOffset >= scrollingLines.size()) continue;

            int layoutLine = scrollingLines.get(scrollingLineOffset++);
            renderLayoutLine(row, layout.get(layoutLine), hasPreviousLine, hasNextLine);
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
     * Refresh a specific button in the static scrolling menu.
     * @param slot Slot of the button to refresh
     * @param button The button to refresh
     */
    public void refreshButton(int slot, Button button) {
        MenuInteractionUtil.refreshButton(inventory, slot, button);
    }

    /**
     * Remove a button for a layout symbol.
     * @param symbol The layout symbol
     * @return The removed button, or null if none existed
     */
    public Button removeButton(String symbol) {
        checkSymbol(symbol);
        Button removed = buttons.remove(symbol);
        if (removed != null) {
            buttonById.remove(removed.uuid());
            removed.onAddToInventory(null);
        }
        return removed;
    }

    /**
     * Remove a button for a layout symbol.
     * @param symbol The layout symbol
     * @return The removed button, or null if none existed
     */
    public Button removeButton(char symbol) {
        return removeButton(String.valueOf(symbol));
    }

    /**
     * Get the button for a layout symbol.
     * @param symbol The layout symbol
     * @return The button, or null if none exists
     */
    @Contract(pure = true)
    public Button getButton(String symbol) {
        checkSymbol(symbol);
        return buttons.get(symbol);
    }

    /**
     * Get the button for a layout symbol.
     * @param symbol The layout symbol
     * @return The button, or null if none exists
     */
    @Contract(pure = true)
    public Button getButton(char symbol) {
        return getButton(String.valueOf(symbol));
    }

    /**
     * Check if a button exists for a layout symbol.
     * @param symbol The layout symbol
     * @return true if a button exists for the symbol
     */
    @Contract(pure = true)
    public boolean hasButton(String symbol) {
        checkSymbol(symbol);
        return buttons.containsKey(symbol);
    }

    /**
     * Check if a button exists for a layout symbol.
     * @param symbol The layout symbol
     * @return true if a button exists for the symbol
     */
    @Contract(pure = true)
    public boolean hasButton(char symbol) {
        return hasButton(String.valueOf(symbol));
    }

    /**
     * Clear all layout button registrations from this menu.
     */
    public void clearButtons() {
        buttons.values().forEach(button -> button.onAddToInventory(null));
        if (nextLineButton != null) nextLineButton.getValue().onAddToInventory(null);
        if (previousLineButton != null) previousLineButton.getValue().onAddToInventory(null);
        detachRenderedButtons();
        buttons.clear();
        buttonById.clear();
        refreshingButtons.clear();
    }

    /**
     * Get the total number of layout buttons in this menu.
     * @return The number of layout buttons
     */
    @Contract(pure = true)
    public int getButtonCount() {
        return buttons.size();
    }

    /**
     * Change the current scroll line.
     * @param line The line number to change to
     */
    public void changeLine(int line) {
        this.line = clampLine(line, getMaxLine());
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
     * Get the maximum scroll line for the current layout and visible rows.
     * @return The maximum scroll line
     */
    @Contract(pure = true)
    public int getMaxLine() {
        if (layout.isEmpty()) return 0;

        validateStaticLineCount();
        int scrollRows = rows - staticLines.size();
        return maxLine(scrollingLines().size(), scrollRows);
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

    private void renderLayoutLine(int row, List<String> line, boolean hasPreviousLine, boolean hasNextLine) {
        for (int column = 0; column < COLUMNS; column++) {
            Button button = buttonForSymbol(line.get(column), hasPreviousLine, hasNextLine);
            if (button == null) continue;

            renderButtonToSlot(row * COLUMNS + column, button);
        }
    }

    private Button buttonForSymbol(String symbol, boolean hasPreviousLine, boolean hasNextLine) {
        if (previousLineButton != null && previousLineButton.getKey().equals(symbol) && hasPreviousLine) {
            return previousLineButton.getValue();
        }

        if (nextLineButton != null && nextLineButton.getKey().equals(symbol) && hasNextLine) {
            return nextLineButton.getValue();
        }

        return buttons.get(symbol);
    }

    private void renderButtonToSlot(int slot, Button button) {
        buttonById.put(button.uuid(), button);
        renderedButtons.add(button);

        if (button.refreshIntervalTicks() > 0L) {
            refreshingButtons.put(slot, button);
        }

        ItemStack stack = button.item(player);
        if (stack == null || stack.getType().isAir()) return;

        MenuInteractionUtil.tagButtonItem(stack, button.uuid());

        SafeUtil.setInventoryItem(inventory, slot, stack);
        button.onAddToInventory(inventory);
    }

    private void detachRenderedButtons() {
        renderedButtons.forEach(button -> button.onAddToInventory(null));
        renderedButtons.clear();
    }

    private List<String> parseLayoutLine(String line, int index) {
        Preconditions.checkArgument(line != null, "Layout line " + index + " cannot be null.");

        List<String> symbols = new ArrayList<>();
        String trimmed = line.trim();

        if (trimmed.contains(" ")) {
            symbols.addAll(Arrays.asList(trimmed.split("\\s+")));
        } else {
            for (int i = 0; i < trimmed.length(); i++) {
                symbols.add(String.valueOf(trimmed.charAt(i)));
            }
        }

        Preconditions.checkArgument(
                symbols.size() == COLUMNS,
                "Layout line " + index + " must contain exactly " + COLUMNS + " symbols."
        );
        return symbols;
    }

    private Map<Integer, Integer> staticLinesByDisplayRow() {
        validateStaticLineCount();
        validateStaticLines();

        Map<Integer, Integer> linesByDisplayRow = new HashMap<>();
        Set<Integer> usedRows = new HashSet<>();
        int lastLayoutLine = layout.size() - 1;

        if (staticLines.contains(0)) {
            assignStaticLine(0, 0, linesByDisplayRow, usedRows);
        }

        if (lastLayoutLine > 0 && staticLines.contains(lastLayoutLine)) {
            assignStaticLine(lastLayoutLine, rows - 1, linesByDisplayRow, usedRows);
        }

        for (Integer staticLine : staticLines) {
            if (staticLine == 0 || staticLine == lastLayoutLine) continue;

            assignStaticLine(staticLine, desiredStaticDisplayRow(staticLine), linesByDisplayRow, usedRows);
        }

        return linesByDisplayRow;
    }

    private void assignStaticLine(
            int staticLine,
            int desiredRow,
            Map<Integer, Integer> linesByDisplayRow,
            Set<Integer> usedRows
    ) {
        int row = nearestAvailableRow(desiredRow, usedRows);
        usedRows.add(row);
        linesByDisplayRow.put(row, staticLine);
    }

    private int desiredStaticDisplayRow(int staticLine) {
        if (layout.size() <= rows) return Math.min(staticLine, rows - 1);

        double ratio = (double) staticLine / (double) (layout.size() - 1);
        return (int) Math.round(ratio * (rows - 1));
    }

    private int nearestAvailableRow(int desiredRow, Set<Integer> usedRows) {
        int clamped = Math.max(0, Math.min(desiredRow, rows - 1));
        for (int distance = 0; distance < rows; distance++) {
            int down = clamped + distance;
            if (down < rows && !usedRows.contains(down)) return down;

            int up = clamped - distance;
            if (distance > 0 && up >= 0 && !usedRows.contains(up)) return up;
        }

        throw new IllegalStateException("No available static row.");
    }

    private List<Integer> scrollingLines() {
        List<Integer> scrollingLines = new ArrayList<>();
        for (int i = 0; i < layout.size(); i++) {
            if (staticLines.contains(i)) continue;

            scrollingLines.add(i);
        }
        return scrollingLines;
    }

    private int maxLine(int scrollingLineCount, int scrollRows) {
        if (scrollRows <= 0 || scrollingLineCount <= scrollRows) return 0;

        return scrollingLineCount - scrollRows;
    }

    private int clampLine(int line, int maxLine) {
        return Math.max(0, Math.min(line, maxLine));
    }

    private void validateStaticLineCount() {
        Preconditions.checkArgument(staticLines.size() <= rows, "Static line count cannot exceed visible rows.");
    }

    private void validateStaticLines() {
        validateStaticLineCount();
        if (layout.isEmpty()) return;

        for (Integer staticLine : staticLines) {
            Preconditions.checkArgument(
                    staticLine < layout.size(),
                    "Static line " + staticLine + " is outside layout size " + layout.size() + "."
            );
        }
    }

    private void checkSymbol(String symbol) {
        Preconditions.checkArgument(symbol != null && !symbol.isBlank(), "Symbol cannot be blank.");
    }

    private Inventory createInventory() {
        StaticScrollingMenuHolder holder = new StaticScrollingMenuHolder(this);
        Inventory created = Bukkit.createInventory(holder, rows * COLUMNS, title);
        holder.setInventory(created);
        return created;
    }
}
