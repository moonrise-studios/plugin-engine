package gg.moonrise.engine.paper.dialog.canvas.theme;

/**
 * A glyph positioned relative to the start of the element it belongs to.
 * @param x the horizontal offset in pixels
 * @param glyph the glyph
 */
public record PlacedGlyph(int x, ThemeGlyph glyph) {
}
