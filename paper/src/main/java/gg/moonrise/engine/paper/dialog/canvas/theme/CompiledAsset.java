package gg.moonrise.engine.paper.dialog.canvas.theme;

import java.util.ArrayList;
import java.util.List;

/**
 * A theme asset after compilation: the glyphs that draw it and how to place them.
 */
public final class CompiledAsset {

    private final String name;
    private final ThemeAsset.Kind kind;
    private final int height;
    private final int topOffset;
    private final List<PlacedGlyph> fixedGlyphs;
    private final int fixedWidth;
    private final ThemeGlyph left;
    private final ThemeGlyph right;
    private final ThemeGlyph[] middles;

    private CompiledAsset(
            String name,
            ThemeAsset.Kind kind,
            int height,
            int topOffset,
            List<PlacedGlyph> fixedGlyphs,
            int fixedWidth,
            ThemeGlyph left,
            ThemeGlyph right,
            ThemeGlyph[] middles
    ) {
        this.name = name;
        this.kind = kind;
        this.height = height;
        this.topOffset = topOffset;
        this.fixedGlyphs = fixedGlyphs;
        this.fixedWidth = fixedWidth;
        this.left = left;
        this.right = right;
        this.middles = middles;
    }

    /**
     * Creates a fixed-size compiled asset.
     * @param name the asset name
     * @param height the painted height
     * @param topOffset the offset from the top of the text line
     * @param glyphs the horizontal chunks
     * @param width the natural width
     * @return the compiled asset
     */
    public static CompiledAsset fixed(String name, int height, int topOffset, List<PlacedGlyph> glyphs, int width) {
        return new CompiledAsset(name, ThemeAsset.Kind.FIXED, height, topOffset, List.copyOf(glyphs), width, null, null, null);
    }

    /**
     * Creates a stretchable compiled asset.
     * @param name the asset name
     * @param height the painted height
     * @param topOffset the offset from the top of the text line
     * @param left the left cap, or {@code null} when the cap is empty
     * @param right the right cap, or {@code null} when the cap is empty
     * @param middles middle pieces where index {@code k} is {@code 2^k} pixels wide
     * @return the compiled asset
     */
    public static CompiledAsset stretch(String name, int height, int topOffset, ThemeGlyph left, ThemeGlyph right, ThemeGlyph[] middles) {
        return new CompiledAsset(name, ThemeAsset.Kind.STRETCH, height, topOffset, List.of(), 0, left, right, middles.clone());
    }

    public String name() {
        return name;
    }

    public ThemeAsset.Kind kind() {
        return kind;
    }

    public int height() {
        return height;
    }

    /**
     * Offset of the artwork's top edge from the top of the text line it is placed on.
     * @return the offset in pixels
     */
    public int topOffset() {
        return topOffset;
    }

    /**
     * Smallest width this asset can be drawn at.
     * @return the width in pixels
     */
    public int minimumWidth() {
        if (kind == ThemeAsset.Kind.FIXED) return fixedWidth;

        return capWidth(left) + capWidth(right) + 1;
    }

    /**
     * Natural width for fixed assets, or the minimum width for stretchable assets.
     * @return the width in pixels
     */
    public int naturalWidth() {
        return minimumWidth();
    }

    /**
     * Lays out the glyphs needed to draw this asset.
     * @param width the requested width; ignored for fixed assets
     * @return the glyphs, positioned relative to the asset's left edge
     */
    public List<PlacedGlyph> layout(int width) {
        if (kind == ThemeAsset.Kind.FIXED) return fixedGlyphs;

        int target = Math.max(width, minimumWidth());
        List<PlacedGlyph> glyphs = new ArrayList<>();
        int x = 0;
        if (left != null) {
            glyphs.add(new PlacedGlyph(x, left));
            x += left.width();
        }

        int remaining = target - capWidth(left) - capWidth(right);
        for (int k = middles.length - 1; k >= 0 && remaining > 0; k--) {
            int pieceWidth = 1 << k;
            while (remaining >= pieceWidth) {
                glyphs.add(new PlacedGlyph(x, middles[k]));
                x += pieceWidth;
                remaining -= pieceWidth;
            }
        }

        if (right != null) glyphs.add(new PlacedGlyph(x, right));

        return glyphs;
    }

    private static int capWidth(ThemeGlyph cap) {
        return cap == null ? 0 : cap.width();
    }
}
