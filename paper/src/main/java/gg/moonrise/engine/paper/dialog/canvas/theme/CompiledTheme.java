package gg.moonrise.engine.paper.dialog.canvas.theme;

import net.kyori.adventure.key.Key;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Server-side metadata produced by {@link ThemeCompiler}, matching the generated resource pack exactly.
 */
public final class CompiledTheme {

    /**
     * Height of one dialog text line in GUI pixels.
     */
    public static final int LINE_HEIGHT = 9;

    /**
     * Largest supported player head scale; scale {@code n} draws an {@code 8n x 8n} face.
     */
    public static final int MAX_HEAD_SCALE = 3;

    private final DialogTheme theme;
    private final Key font;
    private final Map<String, CompiledAsset> assets;
    private final List<PlacedGlyph> panelTop;
    private final List<List<PlacedGlyph>> panelMiddle;
    private final List<List<PlacedGlyph>> panelBottom;
    private final ThemeGlyph[][] headPixels;
    private final char[] positiveSpaces;
    private final char[] negativeSpaces;
    private final Map<Character, Integer> advances;
    private final ResourcePackExport pack;
    private final String buildHash;

    public CompiledTheme(
            DialogTheme theme,
            Key font,
            Map<String, CompiledAsset> assets,
            List<PlacedGlyph> panelTop,
            List<List<PlacedGlyph>> panelMiddle,
            List<List<PlacedGlyph>> panelBottom,
            ThemeGlyph[][] headPixels,
            char[] positiveSpaces,
            char[] negativeSpaces,
            Map<Character, Integer> advances,
            ResourcePackExport pack,
            String buildHash
    ) {
        this.theme = theme;
        this.font = font;
        this.assets = Map.copyOf(assets);
        this.panelTop = List.copyOf(panelTop);
        this.panelMiddle = List.copyOf(panelMiddle);
        this.panelBottom = List.copyOf(panelBottom);
        this.headPixels = headPixels;
        this.positiveSpaces = positiveSpaces.clone();
        this.negativeSpaces = negativeSpaces.clone();
        this.advances = Map.copyOf(advances);
        this.pack = pack;
        this.buildHash = buildHash;
    }

    public DialogTheme theme() {
        return theme;
    }

    /**
     * Font holding every glyph and space of this theme.
     * @return the font key
     */
    public Key font() {
        return font;
    }

    public ResourcePackExport pack() {
        return pack;
    }

    public UUID packId() {
        return pack.packId();
    }

    /**
     * Hash of the generated pack content.
     * @return the lowercase hex hash
     */
    public String buildHash() {
        return buildHash;
    }

    public Map<String, CompiledAsset> assets() {
        return assets;
    }

    /**
     * Looks up a named asset.
     * @param name the asset name
     * @return the compiled asset
     * @throws IllegalArgumentException when the theme has no such asset
     */
    public CompiledAsset asset(String name) {
        CompiledAsset asset = assets.get(name);
        if (asset == null) {
            throw new IllegalArgumentException("Dialog theme " + theme.namespace() + ":" + theme.id() + " has no asset named " + name);
        }

        return asset;
    }

    /**
     * Glyphs that draw the background panel for a screen of the given height.
     * <p>
     * Every glyph is placed on the first text line; each strip uses a glyph whose ascent pushes it
     * down to its own line, so no strip shares a line with interactive content.
     * @param lines the number of text lines the screen uses
     * @return the panel glyphs in draw order
     */
    public List<PlacedGlyph> panel(int lines) {
        if (lines < 2 || lines > theme.maxLines()) {
            throw new IllegalArgumentException("Panel height must be between 2 and " + theme.maxLines() + " lines, was " + lines);
        }

        List<PlacedGlyph> glyphs = new ArrayList<>(panelTop);
        for (int line = 1; line < lines - 1; line++) {
            glyphs.addAll(panelMiddle.get(line - 1));
        }
        glyphs.addAll(panelBottom.get(lines - 2));
        return glyphs;
    }

    /**
     * Glyph for one row of a player head pixel grid.
     * @param scale the head scale, from 1 to {@link #MAX_HEAD_SCALE}
     * @param row the pixel row, from 0 to 7
     * @return the glyph
     */
    public ThemeGlyph headPixel(int scale, int row) {
        if (scale < 1 || scale > MAX_HEAD_SCALE) {
            throw new IllegalArgumentException("Head scale must be between 1 and " + MAX_HEAD_SCALE);
        }

        return headPixels[scale - 1][row];
    }

    /**
     * Encodes a horizontal cursor movement as space glyphs.
     * @param advance the movement in pixels; negative moves left
     * @return the space characters, empty for zero
     */
    public String spaces(int advance) {
        if (advance == 0) return "";

        char[] alphabet = advance > 0 ? positiveSpaces : negativeSpaces;
        int remaining = Math.abs(advance);
        StringBuilder builder = new StringBuilder();
        for (int power = alphabet.length - 1; power >= 0; power--) {
            int size = 1 << power;
            while (remaining >= size) {
                builder.append(alphabet[power]);
                remaining -= size;
            }
        }

        return builder.toString();
    }

    /**
     * Advance of a theme character, including spaces.
     * @param character the character
     * @return the advance, or empty when the character is not part of this theme
     */
    public OptionalInt advance(char character) {
        Integer advance = advances.get(character);
        return advance == null ? OptionalInt.empty() : OptionalInt.of(advance);
    }
}
