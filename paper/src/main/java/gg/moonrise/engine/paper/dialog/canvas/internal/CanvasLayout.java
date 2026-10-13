package gg.moonrise.engine.paper.dialog.canvas.internal;

import gg.moonrise.engine.paper.dialog.canvas.HeadPixels;
import gg.moonrise.engine.paper.dialog.canvas.theme.CompiledAsset;
import gg.moonrise.engine.paper.dialog.canvas.theme.CompiledTheme;
import gg.moonrise.engine.paper.dialog.canvas.theme.PlacedGlyph;
import gg.moonrise.engine.paper.dialog.canvas.theme.ThemeGlyph;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Collects positioned glyphs per text line and composes them into dialog text.
 * <p>
 * Internal; not part of the supported API. Every composed line advances exactly the theme width, so
 * the client's per-line centring puts every line's left edge at the same position. Elements are
 * emitted in insertion order with space glyphs moving the cursor between them; callers insert
 * elements left to right and backgrounds before foregrounds so draw order and click lookup match.
 */
public final class CanvasLayout {

    private static final TextReplacementConfig LINE_BREAKS = TextReplacementConfig.builder()
            .match("\\r\\n|[\\r\\n]")
            .replacement(" ")
            .build();

    private final CompiledTheme theme;
    private final TextMeasurer measurer;
    private final List<List<Segment>> lines = new ArrayList<>();

    public CanvasLayout(CompiledTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.measurer = new TextMeasurer(theme);
    }

    public CompiledTheme theme() {
        return theme;
    }

    public TextMeasurer measurer() {
        return measurer;
    }

    public int contentLeft() {
        return theme.theme().padding();
    }

    public int contentWidth() {
        return theme.theme().contentWidth();
    }

    /**
     * Places a component on a line.
     * @param line the text line
     * @param x the horizontal position in pixels from the canvas' left edge
     * @param component the component
     * @param advance the component's total advance
     */
    public void place(int line, int x, Component component, int advance) {
        if (line < 0) throw new IllegalArgumentException("Line must not be negative");

        while (lines.size() <= line) {
            lines.add(new ArrayList<>());
        }
        lines.get(line).add(new Segment(x, component, advance));
    }

    /**
     * Places default-font text.
     * @param line the text line
     * @param x the horizontal position
     * @param text the text
     * @param interaction click and hover behaviour
     * @return the text's advance
     */
    public int placeText(int line, int x, Component text, Interaction interaction) {
        Component singleLine = singleLine(text);
        Component styled = interaction.apply(singleLine.colorIfAbsent(theme.theme().textColor()));
        if (!theme.theme().textShadow()) styled = styled.shadowColor(ShadowColor.none());

        int advance = measurer.width(singleLine);
        place(line, x, styled, advance);
        return advance;
    }

    /**
     * Replaces line breaks with spaces; a break inside a composed line would push every later line down.
     * @param text the text
     * @return the text on one line
     */
    public static Component singleLine(Component text) {
        return text.replaceText(LINE_BREAKS);
    }

    /**
     * Places theme artwork.
     * @param line the text line
     * @param x the horizontal position
     * @param asset the asset
     * @param width the requested width, used by stretchable assets
     * @param interaction click and hover behaviour
     */
    public void placeAsset(int line, int x, CompiledAsset asset, int width, Interaction interaction) {
        for (PlacedGlyph placed : asset.layout(width)) {
            place(line, x + placed.x(), interaction.apply(glyph(placed.glyph())), placed.glyph().advance());
        }
    }

    /**
     * Places a themed button: background artwork with a centred label on top.
     * @param line the text line holding the label
     * @param x the horizontal position
     * @param width the button width
     * @param asset the background asset
     * @param label the label
     * @param interaction click and hover behaviour, applied to both background and label
     */
    public void placeButton(int line, int x, int width, CompiledAsset asset, Component label, Interaction interaction) {
        Component fitted = measurer.truncate(singleLine(label), Math.max(0, width - 4));
        placeAsset(line, x, asset, width, interaction);
        placeText(line, x + (width - measurer.width(fitted)) / 2, fitted, interaction);
    }

    /**
     * Places a player face drawn from coloured pixel glyphs.
     * @param line the text line holding the top of the face
     * @param x the horizontal position
     * @param pixels the face pixels
     * @param scale the pixel size, from 1 to {@link CompiledTheme#MAX_HEAD_SCALE}
     * @param interaction click and hover behaviour
     */
    public void placeHead(int line, int x, HeadPixels pixels, int scale, Interaction interaction) {
        TextComponent.Builder head = Component.text()
                .font(theme.font())
                .shadowColor(ShadowColor.none());

        int cursor = 0;
        for (int row = 0; row < 8; row++) {
            ThemeGlyph glyph = theme.headPixel(scale, row);
            for (int column = 0; column < 8; column++) {
                int target = column * scale;
                head.append(Component.text(theme.spaces(target - cursor) + glyph.text(), TextColor.color(pixels.rgb(column, row))));
                cursor = target + glyph.advance();
            }
        }

        place(line, x, interaction.apply(head.build()), cursor);
    }

    /**
     * Places the background panel on the first line.
     * @param totalLines the number of lines the screen uses
     */
    public void placePanel(int totalLines) {
        for (PlacedGlyph placed : theme.panel(totalLines)) {
            place(0, placed.x(), glyph(placed.glyph()), placed.glyph().advance());
        }
    }

    public int lineCount() {
        return lines.size();
    }

    /**
     * Composes the placed segments into one component per line.
     * @param totalLines the number of lines to emit; lines without segments are filled with space
     * @return the lines
     */
    public List<Component> compose(int totalLines) {
        int width = theme.theme().width();
        List<Component> composed = new ArrayList<>(totalLines);
        for (int index = 0; index < totalLines; index++) {
            List<Segment> segments = index < lines.size() ? lines.get(index) : List.of();
            TextComponent.Builder line = Component.text();
            int cursor = 0;
            for (Segment segment : segments) {
                if (segment.x() != cursor) line.append(space(segment.x() - cursor));
                line.append(segment.component());
                cursor = segment.x() + segment.advance();
            }
            if (cursor != width) line.append(space(width - cursor));

            composed.add(line.build());
        }

        return composed;
    }

    private Component glyph(ThemeGlyph glyph) {
        return Component.text(glyph.text())
                .font(theme.font())
                .color(TextColor.color(0xFFFFFF))
                .shadowColor(ShadowColor.none());
    }

    private Component space(int advance) {
        return Component.text(theme.spaces(advance)).font(theme.font());
    }

    /**
     * A component placed at an absolute horizontal position.
     * @param x the position in pixels
     * @param component the component
     * @param advance the component's total advance
     */
    public record Segment(int x, Component component, int advance) {
    }
}
