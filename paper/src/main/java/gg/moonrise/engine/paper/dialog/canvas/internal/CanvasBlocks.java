package gg.moonrise.engine.paper.dialog.canvas.internal;

import gg.moonrise.engine.paper.dialog.canvas.DialogAlign;
import gg.moonrise.engine.paper.dialog.canvas.DialogCard;
import gg.moonrise.engine.paper.dialog.canvas.theme.CompiledAsset;
import gg.moonrise.engine.paper.dialog.canvas.theme.ThemeAsset;
import net.kyori.adventure.text.Component;

import java.util.List;

/**
 * Simple canvas blocks without their own configuration API.
 * <p>
 * Internal; not part of the supported API.
 */
public final class CanvasBlocks {

    private CanvasBlocks() {
    }

    /**
     * Lines of text, each shortened to the content width.
     * @param lines the lines
     * @param align the alignment
     * @return the block
     */
    public static CanvasBlock text(List<Component> lines, DialogAlign align) {
        return new Text(List.copyOf(lines), align);
    }

    /**
     * Empty lines.
     * @param lines the number of lines
     * @return the block
     */
    public static CanvasBlock spacer(int lines) {
        return new Spacer(lines);
    }

    /**
     * Theme artwork; stretchable assets fill the content width.
     * @param asset the asset
     * @param align the alignment
     * @return the block
     */
    public static CanvasBlock asset(CompiledAsset asset, DialogAlign align) {
        return new Asset(asset, align);
    }

    /**
     * A single card at its natural width.
     * @param card the card
     * @param align the alignment
     * @return the block
     */
    public static CanvasBlock card(DialogCard card, DialogAlign align) {
        return new Card(card, align);
    }

    private record Text(List<Component> lines, DialogAlign align) implements CanvasBlock {

        @Override
        public int render(CanvasLayout layout, int line) {
            for (int i = 0; i < lines.size(); i++) {
                Component fitted = layout.measurer().truncate(CanvasLayout.singleLine(lines.get(i)), layout.contentWidth());
                int x = align.position(layout.contentLeft(), layout.contentWidth(), layout.measurer().width(fitted));
                layout.placeText(line + i, x, fitted, Interaction.NONE);
            }
            return lines.size();
        }

        @Override
        public void outline(CanvasOutline outline) {
            for (Component text : lines) {
                outline.line(text);
            }
        }
    }

    private record Spacer(int lines) implements CanvasBlock {

        @Override
        public int render(CanvasLayout layout, int line) {
            return lines;
        }

        @Override
        public void outline(CanvasOutline outline) {
            outline.blank();
        }
    }

    private record Asset(CompiledAsset asset, DialogAlign align) implements CanvasBlock {

        @Override
        public int render(CanvasLayout layout, int line) {
            int width = asset.kind() == ThemeAsset.Kind.STRETCH
                    ? layout.contentWidth()
                    : asset.naturalWidth();
            int x = align.position(layout.contentLeft(), layout.contentWidth(), width);
            layout.placeAsset(line, x, asset, width, Interaction.NONE);
            return DialogCard.assetLines(asset);
        }

        @Override
        public void outline(CanvasOutline outline) {
            // Artwork has no text form
        }
    }

    private record Card(DialogCard card, DialogAlign align) implements CanvasBlock {

        @Override
        public int render(CanvasLayout layout, int line) {
            int width = Math.min(Math.max(1, card.naturalWidth(layout)), layout.contentWidth());
            card.render(layout, line, align.position(layout.contentLeft(), layout.contentWidth(), width), width);
            return card.lines();
        }

        @Override
        public void outline(CanvasOutline outline) {
            card.outline(outline);
        }
    }
}
