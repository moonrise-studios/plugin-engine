package gg.moonrise.engine.paper.dialog.canvas;

import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasAction;
import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasBlock;
import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasLayout;
import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasOutline;
import gg.moonrise.engine.paper.dialog.canvas.internal.Interaction;
import gg.moonrise.engine.paper.dialog.canvas.theme.CompiledAsset;
import gg.moonrise.engine.paper.dialog.canvas.theme.DialogTheme;
import net.kyori.adventure.text.Component;

import java.util.Objects;

/**
 * A themed button drawn in the dialog body.
 * <p>
 * The background artwork and the label share the click action. Clicks are reliable on the label's
 * text line on every supported client; artwork above or below that line may not be clickable on older clients.
 * Without the theme's artwork, the button is a native dialog button or a Bedrock form button.
 */
public final class DialogButton implements CanvasBlock {

    private static final int LABEL_PADDING = 6;

    private final DialogCanvas canvas;
    private final Component label;
    private final CanvasAction action;
    private Component tooltip;
    private int width;
    private boolean selected;
    private String asset;
    private DialogAlign align = DialogAlign.CENTER;

    public DialogButton(DialogCanvas canvas, Component label, Runnable action) {
        this(canvas, label, CanvasAction.run(action));
    }

    public DialogButton(DialogCanvas canvas, Component label, CanvasAction action) {
        this.canvas = Objects.requireNonNull(canvas, "canvas");
        this.label = Objects.requireNonNull(label, "label");
        this.action = Objects.requireNonNull(action, "action");
    }

    public DialogButton tooltip(Component tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public DialogButton tooltip(String tooltip) {
        return tooltip(canvas.component(tooltip));
    }

    /**
     * Fixes the button width instead of fitting the label.
     * @param width the width in pixels
     * @return this button
     */
    public DialogButton width(int width) {
        if (width < 1) throw new IllegalArgumentException("Button width must be positive");

        this.width = width;
        return this;
    }

    /**
     * Draws the button with the theme's selected artwork.
     * @param selected whether the button is selected
     * @return this button
     */
    public DialogButton selected(boolean selected) {
        this.selected = selected;
        return this;
    }

    /**
     * Draws the button with a custom stretchable theme asset.
     * @param asset the asset name
     * @return this button
     */
    public DialogButton asset(String asset) {
        this.asset = Objects.requireNonNull(asset, "asset");
        return this;
    }

    public DialogButton align(DialogAlign align) {
        this.align = Objects.requireNonNull(align, "align");
        return this;
    }

    @Override
    public int render(CanvasLayout layout, int line) {
        String name = asset != null ? asset : selected ? DialogTheme.BUTTON_SELECTED : DialogTheme.BUTTON;
        CompiledAsset background = layout.theme().asset(name);
        Component text = selected ? label.colorIfAbsent(layout.theme().theme().selectedTextColor()) : label;

        int natural = Math.max(layout.measurer().width(text) + LABEL_PADDING * 2, background.minimumWidth());
        int buttonWidth = Math.min(width > 0 ? width : natural, layout.contentWidth());
        int x = align.position(layout.contentLeft(), layout.contentWidth(), buttonWidth);

        layout.placeButton(line + 1, x, buttonWidth, background, text, new Interaction(canvas.click(action), tooltip));
        return 3;
    }

    @Override
    public void outline(CanvasOutline outline) {
        outline.button(new CanvasOutline.Button(label, tooltip, action, selected, null, null));
    }
}
