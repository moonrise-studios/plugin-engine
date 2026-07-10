package gg.moonrise.engine.paper.gui;

import com.google.common.base.Preconditions;
import gg.moonrise.engine.message.util.MiniMessageUtil;
import gg.moonrise.engine.paper.gui.button.Button;
import gg.moonrise.engine.paper.gui.holder.StaticScrollingMenuHolder;
import gg.moonrise.engine.paper.gui.util.MenuInteractionUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.Contract;

import java.util.*;

/**
 * Represents a chest-based static scrolling menu for players.
 * This class renders a scrolling viewport over layout lines while configured
 * static lines stay pinned in the visible inventory. Vertical scrolling treats
 * layout rows as lines; horizontal scrolling treats layout columns as lines.
 */
public abstract class StaticScrollingMenu implements ChestInterface {

    private static final int COLUMNS = 9;

    private Component title = Component.text("Static Scrolling Menu");
    private int rows = 1;
    protected int line = 0;
    private int layoutWidth = COLUMNS;
    private ScrollDirection scrollDirection = ScrollDirection.VERTICAL;

    private final Player player;

    private final List<List<String>> layout = new ArrayList<>();
    private final Set<Integer> staticLines = new TreeSet<>();
    private final Map<String, Button> buttons = new HashMap<>();
    private final Map<UUID, Button> buttonById = new HashMap<>();
    private final Map<Integer, Button> refreshingButtons = new HashMap<>();
    private final Set<Button> renderedButtons = new HashSet<>();

    private Map.Entry<String, Button> nextLineButton;
    private Map.Entry<String, Button> previousLineButton;
    private String nextLineFallbackSymbol;
    private String previousLineFallbackSymbol;

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
        validateLayoutDirection();
        validateStaticLineCount();
        line = clampLine(line, getMaxLine());
    }

    /**
     * Set the full virtual layout for this menu.
     * Lines may be space-separated or compact and must use a consistent width.
     * Vertical layouts must be nine symbols wide. Horizontal layouts may be wider
     * than nine symbols but cannot be taller than the visible menu.
     * @param lines The virtual layout lines
     */
    protected void setLayout(List<String> lines) {
        Preconditions.checkArgument(lines != null && !lines.isEmpty(), "Layout must contain at least one line.");

        layout.clear();
        for (int i = 0; i < lines.size(); i++) {
            List<String> parsedLine = parseLayoutLine(lines.get(i), i);
            if (i == 0) {
                layoutWidth = parsedLine.size();
            } else {
                Preconditions.checkArgument(
                        parsedLine.size() == layoutWidth,
                        "Layout line " + i + " must contain exactly " + layoutWidth + " symbols."
                );
            }
            layout.add(parsedLine);
        }

        validateLayoutDirection();
        validateStaticLines();
        line = clampLine(line, getMaxLine());
    }

    /**
     * Set the direction in which this menu scrolls.
     * @param scrollDirection The scroll direction
     */
    protected void setScrollDirection(ScrollDirection scrollDirection) {
        ScrollDirection checkedDirection = Objects.requireNonNull(scrollDirection, "scrollDirection");
        ScrollDirection previousDirection = this.scrollDirection;
        this.scrollDirection = checkedDirection;
        try {
            validateLayoutDirection();
            validateStaticLines();
        } catch (RuntimeException exception) {
            this.scrollDirection = previousDirection;
            throw exception;
        }
        line = clampLine(line, getMaxLine());
    }

    /**
     * Get the direction in which this menu scrolls.
     * @return The scroll direction
     */
    @Contract(pure = true)
    public ScrollDirection getScrollDirection() {
        return scrollDirection;
    }

    /**
     * Set the layout lines that should stay pinned while the other lines scroll.
     * Line indexes refer to rows in vertical mode and columns in horizontal mode.
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
     * Line indexes refer to rows in vertical mode and columns in horizontal mode.
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
     * When hidden, the slot renders the button for the fallback layout symbol.
     * @param symbol The layout symbol
     * @param fallbackSymbol The fallback layout symbol
     * @param button The button to render
     */
    protected void setNextLineButton(String symbol, String fallbackSymbol, Button button) {
        setNextLineButton(symbol, button);
        checkSymbol(fallbackSymbol);
        nextLineFallbackSymbol = fallbackSymbol;
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
     * Set the next-line button rendered for a layout symbol when a next line exists.
     * When hidden, the slot renders the button for the fallback layout symbol.
     * @param symbol The layout symbol
     * @param fallbackSymbol The fallback layout symbol
     * @param button The button to render
     */
    protected void setNextLineButton(char symbol, char fallbackSymbol, Button button) {
        setNextLineButton(String.valueOf(symbol), String.valueOf(fallbackSymbol), button);
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
     * When hidden, the slot renders the button for the fallback layout symbol.
     * @param symbol The layout symbol
     * @param fallbackSymbol The fallback layout symbol
     * @param button The button to render
     */
    protected void setPreviousLineButton(String symbol, String fallbackSymbol, Button button) {
        setPreviousLineButton(symbol, button);
        checkSymbol(fallbackSymbol);
        previousLineFallbackSymbol = fallbackSymbol;
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
     * Set the previous-line button rendered for a layout symbol when a previous line exists.
     * When hidden, the slot renders the button for the fallback layout symbol.
     * @param symbol The layout symbol
     * @param fallbackSymbol The fallback layout symbol
     * @param button The button to render
     */
    protected void setPreviousLineButton(char symbol, char fallbackSymbol, Button button) {
        setPreviousLineButton(String.valueOf(symbol), String.valueOf(fallbackSymbol), button);
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

        if (scrollDirection == ScrollDirection.HORIZONTAL) refreshHorizontal();
        else refreshVertical();
    }

    private void refreshVertical() {
        Map<Integer, Integer> staticLinesByDisplayRow = staticLinesByDisplayLine(layout.size(), rows);
        List<Integer> scrollingLines = scrollingLines(layout.size());
        int scrollRows = rows - staticLinesByDisplayRow.size();
        int maxLine = maxLine(scrollingLines.size(), scrollRows);
        line = clampLine(line, maxLine);

        boolean hasNextLine = line < maxLine;
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

    private void refreshHorizontal() {
        Map<Integer, Integer> staticLinesByDisplayColumn = staticLinesByDisplayLine(layoutWidth, COLUMNS);
        List<Integer> scrollingLines = scrollingLines(layoutWidth);
        int scrollColumns = COLUMNS - staticLinesByDisplayColumn.size();
        int maxLine = maxLine(scrollingLines.size(), scrollColumns);
        line = clampLine(line, maxLine);

        boolean hasNextLine = line < maxLine;
        boolean hasPreviousLine = line > 0;
        int scrollingLineOffset = line;

        for (int column = 0; column < COLUMNS; column++) {
            Integer staticLine = staticLinesByDisplayColumn.get(column);
            if (staticLine != null) {
                renderLayoutColumn(column, staticLine, hasPreviousLine, hasNextLine);
                continue;
            }

            if (scrollingLineOffset >= scrollingLines.size()) continue;

            int layoutLine = scrollingLines.get(scrollingLineOffset++);
            renderLayoutColumn(column, layoutLine, hasPreviousLine, hasNextLine);
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
     * Scroll backward by one line.
     */
    public void previousLine() {
        changeLine(line - 1);
    }

    /**
     * Scroll forward by one line.
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
     * Get the maximum scroll line for the current layout and visible viewport.
     * @return The maximum scroll line
     */
    @Contract(pure = true)
    public int getMaxLine() {
        if (layout.isEmpty()) return 0;

        validateLayoutDirection();
        validateStaticLineCount();
        int visibleScrollingLines = visibleLineCount() - staticLines.size();
        return maxLine(scrollingLines(layoutLineCount()).size(), visibleScrollingLines);
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

    private void renderLayoutColumn(
            int displayColumn,
            int layoutColumn,
            boolean hasPreviousLine,
            boolean hasNextLine
    ) {
        for (int row = 0; row < layout.size(); row++) {
            Button button = buttonForSymbol(layout.get(row).get(layoutColumn), hasPreviousLine, hasNextLine);
            if (button == null) continue;

            renderButtonToSlot(row * COLUMNS + displayColumn, button);
        }
    }

    private Button buttonForSymbol(String symbol, boolean hasPreviousLine, boolean hasNextLine) {
        if (previousLineButton != null && previousLineButton.getKey().equals(symbol)) {
            return hasPreviousLine ? previousLineButton.getValue() : buttons.get(previousLineFallbackSymbol);
        }

        if (nextLineButton != null && nextLineButton.getKey().equals(symbol)) {
            return hasNextLine ? nextLineButton.getValue() : buttons.get(nextLineFallbackSymbol);
        }

        return buttons.get(symbol);
    }

    private void renderButtonToSlot(int slot, Button button) {
        buttonById.put(button.uuid(), button);
        renderedButtons.add(button);

        if (button.refreshIntervalTicks() > 0L) {
            refreshingButtons.put(slot, button);
        }

        MenuInteractionUtil.renderButton(inventory, slot, button, player);
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

        Preconditions.checkArgument(!symbols.isEmpty(), "Layout line " + index + " cannot be empty.");
        return symbols;
    }

    private Map<Integer, Integer> staticLinesByDisplayLine(int layoutLineCount, int visibleLineCount) {
        validateStaticLineCount();
        validateStaticLines();

        Map<Integer, Integer> linesByDisplayLine = new HashMap<>();
        Set<Integer> usedLines = new HashSet<>();
        int lastLayoutLine = layoutLineCount - 1;

        if (staticLines.contains(0)) {
            assignStaticLine(0, 0, visibleLineCount, linesByDisplayLine, usedLines);
        }

        if (lastLayoutLine > 0 && staticLines.contains(lastLayoutLine)) {
            assignStaticLine(lastLayoutLine, visibleLineCount - 1, visibleLineCount, linesByDisplayLine, usedLines);
        }

        for (Integer staticLine : staticLines) {
            if (staticLine == 0 || staticLine == lastLayoutLine) continue;

            assignStaticLine(
                    staticLine,
                    desiredStaticDisplayLine(staticLine, layoutLineCount, visibleLineCount),
                    visibleLineCount,
                    linesByDisplayLine,
                    usedLines
            );
        }

        return linesByDisplayLine;
    }

    private void assignStaticLine(
            int staticLine,
            int desiredLine,
            int visibleLineCount,
            Map<Integer, Integer> linesByDisplayLine,
            Set<Integer> usedLines
    ) {
        int displayLine = nearestAvailableLine(desiredLine, visibleLineCount, usedLines);
        usedLines.add(displayLine);
        linesByDisplayLine.put(displayLine, staticLine);
    }

    private int desiredStaticDisplayLine(int staticLine, int layoutLineCount, int visibleLineCount) {
        if (layoutLineCount <= visibleLineCount) return Math.min(staticLine, visibleLineCount - 1);

        double ratio = (double) staticLine / (double) (layoutLineCount - 1);
        return (int) Math.round(ratio * (visibleLineCount - 1));
    }

    private int nearestAvailableLine(int desiredLine, int visibleLineCount, Set<Integer> usedLines) {
        int clamped = Math.max(0, Math.min(desiredLine, visibleLineCount - 1));
        for (int distance = 0; distance < visibleLineCount; distance++) {
            int down = clamped + distance;
            if (down < visibleLineCount && !usedLines.contains(down)) return down;

            int up = clamped - distance;
            if (distance > 0 && up >= 0 && !usedLines.contains(up)) return up;
        }

        throw new IllegalStateException("No available static line.");
    }

    private List<Integer> scrollingLines(int layoutLineCount) {
        List<Integer> scrollingLines = new ArrayList<>();
        for (int i = 0; i < layoutLineCount; i++) {
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
        Preconditions.checkArgument(
                staticLines.size() <= visibleLineCount(),
                "Static line count cannot exceed visible lines."
        );
    }

    private void validateStaticLines() {
        validateStaticLineCount();
        if (layout.isEmpty()) return;

        for (Integer staticLine : staticLines) {
            Preconditions.checkArgument(
                    staticLine < layoutLineCount(),
                    "Static line " + staticLine + " is outside layout size " + layoutLineCount() + "."
            );
        }
    }

    private void validateLayoutDirection() {
        if (layout.isEmpty()) return;

        if (scrollDirection == ScrollDirection.HORIZONTAL) {
            Preconditions.checkArgument(
                    layout.size() <= rows,
                    "Horizontal layout height cannot exceed visible menu rows."
            );
        } else {
            Preconditions.checkArgument(layoutWidth == COLUMNS, "Vertical layouts must contain exactly 9 columns.");
        }
    }

    private int layoutLineCount() {
        return scrollDirection == ScrollDirection.HORIZONTAL ? layoutWidth : layout.size();
    }

    private int visibleLineCount() {
        return scrollDirection == ScrollDirection.HORIZONTAL ? COLUMNS : rows;
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
