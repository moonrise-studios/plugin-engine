package gg.moonrise.engine.paper.dialog.canvas.internal;

/**
 * A vertically stacked section of a dialog canvas.
 * <p>
 * Internal; not part of the supported API.
 */
public interface CanvasBlock {

    /**
     * Places this block's glyphs.
     * @param layout the layout being built
     * @param line the first text line available to this block
     * @return the number of text lines used
     */
    public int render(CanvasLayout layout, int line);

    /**
     * Describes this block as text lines and buttons, for screens without the theme's artwork.
     * @param outline the outline being built
     */
    public void outline(CanvasOutline outline);
}
