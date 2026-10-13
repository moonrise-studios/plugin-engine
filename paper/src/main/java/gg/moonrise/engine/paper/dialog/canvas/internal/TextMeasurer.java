package gg.moonrise.engine.paper.dialog.canvas.internal;

import gg.moonrise.engine.paper.dialog.canvas.theme.CompiledTheme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Measures text advances the way the client lays out dialog text.
 * <p>
 * Internal; not part of the supported API. Characters in the vanilla ASCII sheet use their exact
 * advances. Other characters in the default font are estimated per code point, and translatable or
 * keybind components are measured by their key, so such text can shift its line slightly off centre. Characters in the theme font use the compiled glyph metrics.
 */
public final class TextMeasurer {

    private static final int[] ASCII_ADVANCES = new int[128];
    private static final String ELLIPSIS = "...";

    static {
        Arrays.fill(ASCII_ADVANCES, 6);
        ASCII_ADVANCES[' '] = 4;
        for (char c : "!',.:;i|".toCharArray()) ASCII_ADVANCES[c] = 2;
        for (char c : "l`".toCharArray()) ASCII_ADVANCES[c] = 3;
        for (char c : "I[]t".toCharArray()) ASCII_ADVANCES[c] = 4;
        for (char c : "\"()*<>fk{}".toCharArray()) ASCII_ADVANCES[c] = 5;
        for (char c : "@~".toCharArray()) ASCII_ADVANCES[c] = 7;
    }

    private final CompiledTheme theme;

    public TextMeasurer(CompiledTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    /**
     * Measures a component, including its children.
     * @param component the component
     * @return the total advance in pixels
     */
    public int width(Component component) {
        return width(component, Style.empty());
    }

    /**
     * Measures a plain string in a given style.
     * @param text the text
     * @param style the effective style
     * @return the advance in pixels
     */
    public int width(String text, Style style) {
        boolean bold = style.decoration(TextDecoration.BOLD) == TextDecoration.State.TRUE;
        boolean themeFont = theme.font().equals(style.font());
        int width = 0;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            width += advance(codePoint, bold, themeFont);
            i += Character.charCount(codePoint);
        }

        return width;
    }

    /**
     * Shortens a component to fit a width, ending it with an ellipsis.
     * <p>
     * A component that already fits is returned unchanged. A shortened component keeps only its root
     * style and plain text, so nested formatting is dropped.
     * @param component the component
     * @param maxWidth the available width in pixels
     * @return the component, possibly shortened
     */
    public Component truncate(Component component, int maxWidth) {
        if (width(component) <= maxWidth) return component;

        Style style = component.style();
        String plain = plain(component);
        int ellipsis = width(ELLIPSIS, style);
        if (ellipsis > maxWidth) return Component.empty();

        StringBuilder builder = new StringBuilder();
        int used = ellipsis;
        for (int i = 0; i < plain.length(); ) {
            int codePoint = plain.codePointAt(i);
            int advance = width(Character.toString(codePoint), style);
            if (used + advance > maxWidth) break;

            builder.appendCodePoint(codePoint);
            used += advance;
            i += Character.charCount(codePoint);
        }

        return Component.text(builder.toString().stripTrailing() + ELLIPSIS, style);
    }

    /**
     * Wraps a component into lines that fit a width, breaking at spaces and at line breaks.
     * <p>
     * Every part keeps its own style, so colours, decorations, and click or hover events survive
     * wrapping. A word wider than a line is broken where it reaches the edge. Translatable, keybind,
     * and other non-text parts are kept whole, since only the client knows their text.
     * @param component the component
     * @param maxWidth the available width in pixels
     * @return the lines
     */
    public List<Component> wrap(Component component, int maxWidth) {
        List<Component> lines = new ArrayList<>();
        if (width(component) <= maxWidth && !plain(component).contains("\n")) {
            lines.add(component);
            return lines;
        }

        List<Glyph> runs = new ArrayList<>();
        flatten(component, Style.empty(), runs);

        List<Glyph> line = new ArrayList<>();
        int used = 0;
        int lastSpace = -1;
        for (Glyph glyph : runs) {
            if (glyph.codePoint() == '\n') {
                lines.add(compose(line));
                line.clear();
                used = 0;
                lastSpace = -1;
                continue;
            }

            int advance = advance(glyph);
            if (used + advance > maxWidth && !line.isEmpty()) {
                List<Glyph> carried = new ArrayList<>();
                if (glyph.codePoint() != ' ' && lastSpace >= 0) {
                    carried.addAll(line.subList(lastSpace + 1, line.size()));
                    line.subList(lastSpace, line.size()).clear();
                }
                lines.add(compose(line));
                line.clear();
                line.addAll(carried);
                used = 0;
                for (Glyph kept : line) used += advance(kept);
                lastSpace = -1;
                if (glyph.codePoint() == ' ') continue;
            }

            if (glyph.codePoint() == ' ') lastSpace = line.size();
            line.add(glyph);
            used += advance;
        }
        lines.add(compose(line));

        return lines;
    }

    private void flatten(Component component, Style inherited, List<Glyph> out) {
        Style effective = component.style().merge(inherited, Style.Merge.Strategy.IF_ABSENT_ON_TARGET);
        if (component instanceof TextComponent text) {
            text.content().codePoints().forEach(codePoint -> out.add(new Glyph(codePoint, effective, null)));
        } else {
            Component whole = component.children(List.of()).style(effective);
            out.add(new Glyph(-1, effective, whole));
        }
        for (Component child : component.children()) {
            flatten(child, effective, out);
        }
    }

    private int advance(Glyph glyph) {
        return glyph.whole() != null
                ? width(glyph.whole())
                : width(Character.toString(glyph.codePoint()), glyph.style());
    }

    private static Component compose(List<Glyph> line) {
        int start = 0;
        int end = line.size();
        while (start < end && line.get(start).codePoint() == ' ') start++;
        while (end > start && line.get(end - 1).codePoint() == ' ') end--;

        TextComponent.Builder builder = Component.text();
        StringBuilder segment = new StringBuilder();
        Style style = null;
        for (int i = start; i < end; i++) {
            Glyph glyph = line.get(i);
            if (glyph.whole() != null) {
                if (style != null && !segment.isEmpty()) builder.append(Component.text(segment.toString(), style));
                segment.setLength(0);
                style = null;
                builder.append(glyph.whole());
                continue;
            }
            if (style != null && !glyph.style().equals(style)) {
                builder.append(Component.text(segment.toString(), style));
                segment.setLength(0);
            }
            style = glyph.style();
            segment.appendCodePoint(glyph.codePoint());
        }
        if (style != null && !segment.isEmpty()) builder.append(Component.text(segment.toString(), style));

        return builder.build();
    }

    /**
     * One character with the style it is drawn in, or a non-text part kept whole.
     * @param codePoint the character, or -1 for a whole part
     * @param style the effective style
     * @param whole the non-text part, or {@code null}
     */
    private record Glyph(int codePoint, Style style, Component whole) {
    }

    private int width(Component component, Style inherited) {
        Style effective = component.style().merge(inherited, Style.Merge.Strategy.IF_ABSENT_ON_TARGET);
        String own = component instanceof TextComponent text ? text.content() : plain(component.children(List.of()));
        int width = width(own, effective);
        for (Component child : component.children()) {
            width += width(child, effective);
        }

        return width;
    }

    private int advance(int character, boolean bold, boolean themeFont) {
        if (themeFont && Character.isBmpCodePoint(character)) {
            OptionalInt advance = theme.advance((char) character);
            if (advance.isPresent()) return advance.getAsInt();
        }

        int advance;
        if (character < ASCII_ADVANCES.length) {
            advance = ASCII_ADVANCES[character];
        } else if (character < 0x2000) {
            advance = 6;
        } else {
            advance = 9;
        }

        if (bold && character != ' ') advance++;
        return advance;
    }

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }
}
