package gg.moonrise.engine.paper.dialog.canvas;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Helpers for grouping entries into rows for {@link DialogCanvas#centeredRows}.
 */
public final class DialogLayouts {

    private DialogLayouts() {
    }

    /**
     * Splits entries into rows of the given sizes, in order.
     * <p>
     * {@code rows(top16, 1, 3, 5, 7)} turns sixteen leaderboard entries into a pyramid. Rows stop when the
     * entries run out; entries beyond the listed sizes continue in rows of the last size.
     * @param entries the entries
     * @param sizes the row sizes
     * @param <E> the entry type
     * @return the rows
     */
    public static <E> List<List<E>> rows(List<? extends E> entries, int... sizes) {
        Objects.requireNonNull(entries, "entries");
        if (sizes.length == 0) throw new IllegalArgumentException("At least one row size is required");
        for (int size : sizes) {
            if (size < 1) throw new IllegalArgumentException("Row sizes must be positive");
        }

        List<List<E>> rows = new ArrayList<>();
        int index = 0;
        int row = 0;
        while (index < entries.size()) {
            int size = sizes[Math.min(row, sizes.length - 1)];
            int end = Math.min(entries.size(), index + size);
            rows.add(List.copyOf(entries.subList(index, end)));
            index = end;
            row++;
        }

        return rows;
    }

    /**
     * Splits entries into rows of {@code columns}; the last row holds the rest and is centred by
     * {@link DialogCanvas#centeredRows}, as vanilla dialogs centre their last row of buttons.
     * @param entries the entries
     * @param columns the entries per row
     * @param <E> the entry type
     * @return the rows
     */
    public static <E> List<List<E>> grid(List<? extends E> entries, int columns) {
        return rows(entries, columns);
    }
}
