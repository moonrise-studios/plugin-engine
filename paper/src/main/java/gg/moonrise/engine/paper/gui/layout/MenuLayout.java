package gg.moonrise.engine.paper.gui.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Slot layout helper for inventory menus.
 * <p>
 * Layout rows are read like the inventory grid. Whitespace is ignored so rows
 * can be written as compact strings or spaced visual patterns.
 */
public final class MenuLayout {

    public static final int CHEST_WIDTH = 9;
    public static final int HOPPER_WIDTH = 5;
    public static final int MAX_CHEST_ROWS = 6;

    private final int width;
    private final int height;
    private final String data;
    private final Map<Character, List<Integer>> slotsByKey;

    private MenuLayout(int width, int height, String data) {
        if (width <= 0) throw new IllegalArgumentException("Layout width must be greater than zero.");
        if (height <= 0) throw new IllegalArgumentException("Layout height must be greater than zero.");
        if (data.length() != width * height) {
            throw new IllegalArgumentException("Layout data length must equal width * height.");
        }

        this.width = width;
        this.height = height;
        this.data = data;
        this.slotsByKey = indexSlots(width, height, data);
    }

    /**
     * Create a chest layout. Each row must contain exactly nine non-whitespace
     * characters after sanitizing.
     *
     * @param rows the layout rows
     * @return the layout
     */
    public static MenuLayout chest(String... rows) {
        MenuLayout layout = of(CHEST_WIDTH, rows);
        if (layout.height() > MAX_CHEST_ROWS) {
            throw new IllegalArgumentException("Chest layouts may have at most six rows.");
        }
        return layout;
    }

    /**
     * Create a hopper layout. The row must contain exactly five non-whitespace
     * characters after sanitizing.
     *
     * @param row the layout row
     * @return the layout
     */
    public static MenuLayout hopper(String row) {
        return of(HOPPER_WIDTH, row);
    }

    /**
     * Create a layout using a fixed row width.
     *
     * @param width the row width
     * @param rows the layout rows
     * @return the layout
     */
    public static MenuLayout of(int width, String... rows) {
        if (rows == null || rows.length == 0) {
            throw new IllegalArgumentException("Layout must contain at least one row.");
        }

        StringBuilder builder = new StringBuilder(width * rows.length);
        for (String row : rows) {
            String sanitized = sanitize(row);
            if (sanitized.length() != width) {
                throw new IllegalArgumentException("Each layout row must contain exactly " + width + " slots.");
            }
            builder.append(sanitized);
        }

        return new MenuLayout(width, rows.length, builder.toString());
    }

    /**
     * Create a layout from a flat data string.
     *
     * @param width the row width
     * @param height the row count
     * @param data the flat layout data
     * @return the layout
     */
    public static MenuLayout of(int width, int height, String data) {
        return new MenuLayout(width, height, sanitize(data));
    }

    /**
     * Get all slots matching a layout key in horizontal order.
     *
     * @param key the layout key
     * @return matching slots
     */
    public List<Integer> slots(char key) {
        return slotsByKey.getOrDefault(key, List.of());
    }

    /**
     * Get all slots matching a layout key in the requested order.
     *
     * @param key the layout key
     * @param order the slot order
     * @return matching slots
     */
    public List<Integer> slots(char key, ContentSlotOrder order) {
        if (order == ContentSlotOrder.HORIZONTAL) return slots(key);

        List<Integer> slots = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int slot = y * width + x;
                if (data.charAt(slot) == key) slots.add(slot);
            }
        }
        return List.copyOf(slots);
    }

    /**
     * Find the first slot for a layout key.
     *
     * @param key the layout key
     * @return the first slot, if present
     */
    public OptionalInt findFirstSlot(char key) {
        List<Integer> slots = slots(key);
        if (slots.isEmpty()) return OptionalInt.empty();
        return OptionalInt.of(slots.getFirst());
    }

    /**
     * Get the first slot for a layout key.
     *
     * @param key the layout key
     * @return the first slot
     */
    public int firstSlot(char key) {
        return findFirstSlot(key)
                .orElseThrow(() -> new IllegalArgumentException("Layout key '" + key + "' has no slots."));
    }

    /**
     * Check whether the layout contains a key.
     *
     * @param key the layout key
     * @return true if present
     */
    public boolean has(char key) {
        return slotsByKey.containsKey(key);
    }

    /**
     * Get all keys used by this layout.
     *
     * @return keys in first-seen order
     */
    public Set<Character> keys() {
        return slotsByKey.keySet();
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int size() {
        return width * height;
    }

    private static Map<Character, List<Integer>> indexSlots(int width, int height, String data) {
        Map<Character, List<Integer>> slots = new LinkedHashMap<>();
        for (int slot = 0; slot < width * height; slot++) {
            char key = data.charAt(slot);
            slots.computeIfAbsent(key, ignored -> new ArrayList<>()).add(slot);
        }

        Map<Character, List<Integer>> indexed = new LinkedHashMap<>();
        for (Character key : slots.keySet()) {
            indexed.put(key, Collections.unmodifiableList(slots.get(key)));
        }
        return Collections.unmodifiableMap(indexed);
    }

    private static String sanitize(String value) {
        if (value == null) throw new IllegalArgumentException("Layout data cannot be null.");

        StringBuilder builder = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (Character.isWhitespace(character)) continue;
            builder.append(character);
        }
        return builder.toString();
    }
}
