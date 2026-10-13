package gg.moonrise.engine.paper.dialog.canvas;

/**
 * Horizontal alignment within the canvas content area.
 */
public enum DialogAlign {
    LEFT,
    CENTER,
    RIGHT;

    /**
     * Calculates where an element starts.
     * @param left the left edge of the available area
     * @param available the available width
     * @param width the element width
     * @return the element's left edge
     */
    public int position(int left, int available, int width) {
        return switch (this) {
            case LEFT -> left;
            case CENTER -> left + (available - width) / 2;
            case RIGHT -> left + available - width;
        };
    }
}
