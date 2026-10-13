package gg.moonrise.engine.paper.dialog.canvas;

import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasBlock;
import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasLayout;
import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasOutline;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Rows of equally sized cards, each row centred independently.
 * <p>
 * For a row of {@code n} cards, card {@code i} is centred at
 * {@code W / 2 + (i - (n - 1) / 2) * (cardWidth + gap)}, so rows of 1, 3, 5, and 7 form a pyramid.
 * All rows share one card width so columns line up.
 * @param <E> the entry type
 */
public final class DialogRows<E> implements CanvasBlock {

    private final DialogCanvas canvas;
    private final List<List<E>> rows;
    private final BiConsumer<? super DialogCard, ? super E> renderer;
    private int gap = 6;
    private int cardWidth;
    private int rowGap = 1;

    public DialogRows(DialogCanvas canvas, List<? extends List<? extends E>> rows, BiConsumer<? super DialogCard, ? super E> renderer) {
        this.canvas = Objects.requireNonNull(canvas, "canvas");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        List<List<E>> copy = new ArrayList<>();
        for (List<? extends E> row : Objects.requireNonNull(rows, "rows")) {
            copy.add(List.copyOf(row));
        }
        this.rows = copy;
    }

    /**
     * Sets the horizontal space between cards.
     * @param gap the gap in pixels
     * @return these rows
     */
    public DialogRows<E> gap(int gap) {
        if (gap < 0) throw new IllegalArgumentException("Card gap must not be negative");

        this.gap = gap;
        return this;
    }

    /**
     * Fixes the card width instead of using the widest card.
     * @param cardWidth the width in pixels
     * @return these rows
     */
    public DialogRows<E> cardWidth(int cardWidth) {
        if (cardWidth < 1) throw new IllegalArgumentException("Card width must be positive");

        this.cardWidth = cardWidth;
        return this;
    }

    /**
     * Sets the empty lines between rows.
     * @param rowGap the number of lines
     * @return these rows
     */
    public DialogRows<E> rowGap(int rowGap) {
        if (rowGap < 0) throw new IllegalArgumentException("Row gap must not be negative");

        this.rowGap = rowGap;
        return this;
    }

    @Override
    public int render(CanvasLayout layout, int line) {
        List<List<DialogCard>> cards = new ArrayList<>();
        int widest = 0;
        int mostCards = 0;
        for (List<E> row : rows) {
            if (row.isEmpty()) continue;

            List<DialogCard> rowCards = new ArrayList<>(row.size());
            for (E entry : row) {
                DialogCard card = new DialogCard(canvas);
                renderer.accept(card, entry);
                rowCards.add(card);
                widest = Math.max(widest, card.naturalWidth(layout));
            }
            cards.add(rowCards);
            mostCards = Math.max(mostCards, rowCards.size());
        }
        if (cards.isEmpty()) return 0;

        int available = layout.contentWidth();
        int width = cardWidth > 0 ? cardWidth : Math.max(16, widest);
        int fitting = (available - gap * (mostCards - 1)) / mostCards;
        if (fitting < 1) throw new IllegalStateException("Dialog row of " + mostCards + " cards does not fit the theme width");
        width = Math.min(width, fitting);

        double center = layout.contentLeft() + available / 2.0;
        int current = line;
        for (int r = 0; r < cards.size(); r++) {
            if (r > 0) current += rowGap;

            List<DialogCard> row = cards.get(r);
            int rowLines = 0;
            for (int i = 0; i < row.size(); i++) {
                double cardCenter = center + (i - (row.size() - 1) / 2.0) * (width + gap);
                int left = (int) Math.round(cardCenter - width / 2.0);
                row.get(i).render(layout, current, left, width);
                rowLines = Math.max(rowLines, row.get(i).lines());
            }
            current += rowLines;
        }

        return current - line;
    }

    @Override
    public void outline(CanvasOutline outline) {
        for (List<E> row : rows) {
            for (E entry : row) {
                DialogCard card = new DialogCard(canvas);
                renderer.accept(card, entry);
                card.outline(outline);
            }
        }
    }
}
