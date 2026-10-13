package gg.moonrise.engine.paper.dialog.canvas;

import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasAction;
import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasLayout;
import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasOutline;
import gg.moonrise.engine.paper.dialog.canvas.internal.Interaction;
import gg.moonrise.engine.paper.dialog.canvas.theme.CompiledAsset;
import gg.moonrise.engine.paper.dialog.canvas.theme.CompiledTheme;
import gg.moonrise.engine.paper.dialog.canvas.theme.DialogTheme;
import gg.moonrise.engine.paper.dialog.canvas.theme.ThemeAsset;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A vertical stack of centred elements, such as a face above a name and a value.
 * <p>
 * Cards are laid out by {@link DialogCanvas#centeredRows} and {@link DialogCanvas#card}. A tooltip or
 * click action set on the card applies to every element except buttons, which keep their own action.
 * <p>
 * Without the theme's artwork, a card with a face or a click action becomes one button whose label is
 * the card's text lines, and its own buttons follow it; any other card becomes its text lines. On a
 * Bedrock form that button shows the face, or else the card's first image when the menu names a
 * Bedrock texture for it.
 */
public final class DialogCard {

    private static final int BUTTON_PADDING = 6;

    private final DialogCanvas canvas;
    private final List<Element> elements = new ArrayList<>();
    private Component tooltip;
    private Runnable onClick;
    private CompiledAsset background;

    public DialogCard(DialogCanvas canvas) {
        this.canvas = Objects.requireNonNull(canvas, "canvas");
    }

    /**
     * Adds a 16 x 16 player face.
     * @param playerId the player
     * @return this card
     */
    public DialogCard head(UUID playerId) {
        return head(playerId, 16);
    }

    /**
     * Adds a player face.
     * @param playerId the player
     * @param size the face size in pixels: 8, 16, or 24
     * @return this card
     */
    public DialogCard head(UUID playerId, int size) {
        Objects.requireNonNull(playerId, "playerId");
        if (size % 8 != 0 || size < 8 || size > 8 * CompiledTheme.MAX_HEAD_SCALE) {
            throw new IllegalArgumentException("Head size must be 8, 16, or 24 pixels");
        }

        elements.add(new Head(playerId, size / 8));
        return this;
    }

    public DialogCard head(OfflinePlayer player, int size) {
        return head(Objects.requireNonNull(player, "player").getUniqueId(), size);
    }

    /**
     * Adds one line of text, shortened to the card width when needed.
     * @param text the text
     * @return this card
     */
    public DialogCard text(Component text) {
        elements.add(new Text(Objects.requireNonNull(text, "text")));
        return this;
    }

    /**
     * Adds one line of MiniMessage text, resolved for the viewer.
     * @param miniMessage the text
     * @return this card
     */
    public DialogCard text(String miniMessage) {
        return text(canvas.component(miniMessage));
    }

    /**
     * Adds theme artwork.
     * @param asset the asset name
     * @return this card
     */
    public DialogCard image(String asset) {
        elements.add(new Image(canvas.theme().asset(asset)));
        return this;
    }

    /**
     * Adds a themed button spanning the card width.
     * @param label the label
     * @param action runs when clicked
     * @return this card
     */
    public DialogCard button(Component label, Runnable action) {
        elements.add(new Button(Objects.requireNonNull(label, "label"), CanvasAction.run(action)));
        return this;
    }

    public DialogCard button(String label, Runnable action) {
        return button(canvas.component(label), action);
    }

    /**
     * Adds a themed button spanning the card width that opens a web page in the viewer's client.
     * <p>
     * Java clients ask before opening it; Bedrock forms cannot open pages, so Bedrock players get
     * the address in chat instead.
     * @param label the label
     * @param url an http or https address
     * @return this card
     */
    public DialogCard link(Component label, String url) {
        elements.add(new Button(Objects.requireNonNull(label, "label"), CanvasAction.openUrl(url)));
        return this;
    }

    public DialogCard link(String label, String url) {
        return link(canvas.component(label), url);
    }

    /**
     * Adds empty lines.
     * @param lines the number of lines
     * @return this card
     */
    public DialogCard spacer(int lines) {
        if (lines < 1) throw new IllegalArgumentException("Spacer must be at least one line");

        elements.add(new Spacer(lines));
        return this;
    }

    /**
     * Draws theme artwork behind the whole card, such as a tile behind an icon and a label.
     * <p>
     * Stretchable artwork spans the card width; fixed artwork is centred. The artwork starts on the
     * card's first line at its own vertical offset, so give tall tiles a negative
     * {@link ThemeAsset#topOffset} to frame the first element. It shares the card's click action and
     * tooltip, and is left out where the theme's artwork is not shown.
     * @param asset the asset name
     * @return this card
     */
    public DialogCard background(String asset) {
        this.background = canvas.theme().asset(asset);
        return this;
    }

    public DialogCard tooltip(Component tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public DialogCard tooltip(String tooltip) {
        return tooltip(canvas.component(tooltip));
    }

    /**
     * Makes every non-button element of the card clickable.
     * @param action runs when clicked
     * @return this card
     */
    public DialogCard onClick(Runnable action) {
        this.onClick = action;
        return this;
    }

    /**
     * Number of text lines the card occupies.
     * @return the line count
     */
    public int lines() {
        int lines = 0;
        for (Element element : elements) {
            lines += element.lines();
        }

        return lines;
    }

    /**
     * Widest element of the card.
     * @param layout the layout used for measuring
     * @return the width in pixels
     */
    public int naturalWidth(CanvasLayout layout) {
        int width = background == null ? 0 : background.minimumWidth();
        for (Element element : elements) {
            width = Math.max(width, element.naturalWidth(layout));
        }

        return width;
    }

    /**
     * Places the card. Internal; called by the canvas renderer.
     * @param layout the layout
     * @param line the first line of the card
     * @param left the card's left edge
     * @param width the card width
     */
    public void render(CanvasLayout layout, int line, int left, int width) {
        Interaction interaction = new Interaction(onClick == null ? null : canvas.action(onClick), tooltip);
        if (background != null) {
            int drawn = background.kind() == ThemeAsset.Kind.STRETCH ? width : background.naturalWidth();
            layout.placeAsset(line, left + (width - drawn) / 2, background, drawn, interaction);
        }
        int current = line;
        for (Element element : elements) {
            element.render(layout, current, left, width, interaction);
            current += element.lines();
        }
    }

    /**
     * Describes the card as text lines and buttons. Internal; called by the canvas renderer.
     * @param outline the outline being built
     */
    public void outline(CanvasOutline outline) {
        List<Component> texts = new ArrayList<>();
        UUID head = null;
        String image = null;
        List<Button> buttons = new ArrayList<>();
        for (Element element : elements) {
            if (element instanceof Text text) texts.add(text.text());
            else if (element instanceof Head face && head == null) head = face.playerId;
            else if (element instanceof Image picture && image == null) image = picture.asset().name();
            else if (element instanceof Button button) buttons.add(button);
        }

        if (onClick != null || head != null) {
            if (!texts.isEmpty() || head != null) {
                Component label = Component.join(JoinConfiguration.newlines(), texts);
                CanvasAction action = onClick == null ? null : CanvasAction.run(onClick);
                outline.button(new CanvasOutline.Button(label, tooltip, action, false, head, image));
            }
        } else {
            for (Component text : texts) {
                outline.line(text);
            }
        }

        for (Button button : buttons) {
            outline.button(new CanvasOutline.Button(button.label, tooltip, button.action, false, null, null));
        }
    }

    private interface Element {

        int lines();

        int naturalWidth(CanvasLayout layout);

        void render(CanvasLayout layout, int line, int left, int width, Interaction interaction);
    }

    private final class Head implements Element {

        private final UUID playerId;
        private final int scale;

        private Head(UUID playerId, int scale) {
            this.playerId = playerId;
            this.scale = scale;
        }

        @Override
        public int lines() {
            return (8 * scale + CompiledTheme.LINE_HEIGHT - 1) / CompiledTheme.LINE_HEIGHT;
        }

        @Override
        public int naturalWidth(CanvasLayout layout) {
            return 8 * scale;
        }

        @Override
        public void render(CanvasLayout layout, int line, int left, int width, Interaction interaction) {
            HeadPixels pixels = canvas.heads().cachedOrPlaceholder(playerId);
            layout.placeHead(line, left + (width - 8 * scale) / 2, pixels, scale, interaction);
        }
    }

    private record Text(Component text) implements Element {

        @Override
        public int lines() {
            return 1;
        }

        @Override
        public int naturalWidth(CanvasLayout layout) {
            return layout.measurer().width(text);
        }

        @Override
        public void render(CanvasLayout layout, int line, int left, int width, Interaction interaction) {
            Component fitted = layout.measurer().truncate(CanvasLayout.singleLine(text), width);
            layout.placeText(line, left + (width - layout.measurer().width(fitted)) / 2, fitted, interaction);
        }
    }

    private record Image(CompiledAsset asset) implements Element {

        @Override
        public int lines() {
            return assetLines(asset);
        }

        @Override
        public int naturalWidth(CanvasLayout layout) {
            return asset.naturalWidth();
        }

        @Override
        public void render(CanvasLayout layout, int line, int left, int width, Interaction interaction) {
            int drawn = asset.kind() == ThemeAsset.Kind.STRETCH ? width : asset.naturalWidth();
            layout.placeAsset(line, left + (width - drawn) / 2, asset, drawn, interaction);
        }
    }

    private final class Button implements Element {

        private final Component label;
        private final CanvasAction action;

        private Button(Component label, CanvasAction action) {
            this.label = label;
            this.action = action;
        }

        @Override
        public int lines() {
            return 3;
        }

        @Override
        public int naturalWidth(CanvasLayout layout) {
            return Math.max(layout.measurer().width(label) + BUTTON_PADDING * 2, layout.theme().asset(DialogTheme.BUTTON).minimumWidth());
        }

        @Override
        public void render(CanvasLayout layout, int line, int left, int width, Interaction interaction) {
            Interaction own = new Interaction(canvas.click(action), interaction.tooltip());
            layout.placeButton(line + 1, left, width, layout.theme().asset(DialogTheme.BUTTON), label, own);
        }
    }

    private record Spacer(int lines) implements Element {

        @Override
        public int naturalWidth(CanvasLayout layout) {
            return 0;
        }

        @Override
        public void render(CanvasLayout layout, int line, int left, int width, Interaction interaction) {
        }
    }

    /**
     * Number of text lines an asset needs below the line it is placed on.
     * @param asset the asset
     * @return the line count, at least one
     */
    public static int assetLines(CompiledAsset asset) {
        int bottom = asset.topOffset() + asset.height();
        return Math.max(1, (bottom + CompiledTheme.LINE_HEIGHT - 1) / CompiledTheme.LINE_HEIGHT);
    }
}
