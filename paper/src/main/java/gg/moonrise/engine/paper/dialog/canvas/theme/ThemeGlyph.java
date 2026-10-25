package gg.moonrise.engine.paper.dialog.canvas.theme;

/**
 * A single compiled font glyph and the metrics the client will use for it.
 * @param character the private-use character assigned to the glyph
 * @param width the painted width in pixels
 * @param height the painted height in pixels
 * @param ascent the bitmap ascent written into the font definition
 * @param advance how far the client moves the text cursor after drawing the glyph
 */
public record ThemeGlyph(char character, int width, int height, int ascent, int advance) {

    /**
     * Returns the glyph as text.
     * @return a one character string
     */
    public String text() {
        return String.valueOf(character);
    }
}
