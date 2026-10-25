package gg.moonrise.engine.paper.dialog.canvas;

import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasAction;
import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasBlock;
import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasBlocks;
import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasOutline;
import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasText;
import gg.moonrise.engine.paper.dialog.canvas.internal.TextMeasurer;
import gg.moonrise.engine.paper.dialog.canvas.theme.CompiledTheme;
import gg.moonrise.engine.paper.dialog.canvas.theme.DialogTheme;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Describes one themed dialog screen as a vertical stack of components.
 * <p>
 * A canvas is created for every render and passed to {@link DialogMenu#render}. Components are laid
 * out top to bottom inside the theme's panel; the engine converts them into font glyphs, spacing, and
 * click callbacks. Click actions run on the player's thread and only while the screen they were
 * rendered on is still the current one.
 * <p>
 * The same description serves every client: Java players with the theme's pack see the artwork, Java
 * players without it see a vanilla dialog, and Bedrock players see a form. The last two show the text
 * and every clickable element as buttons, in the order they were added; see {@link DialogPresentation}.
 */
public final class DialogCanvas {

    private static final int DEFAULT_EXIT_WIDTH = 150;

    private final DialogSession<?> session;
    private final Player viewer;
    private final CompiledTheme theme;
    private final PlayerHeads heads;
    private final TextMeasurer measurer;
    private final List<CanvasBlock> blocks = new ArrayList<>();
    private Component footer;
    private ExitButton exitButton;

    public DialogCanvas(DialogSession<?> session, Player viewer, CompiledTheme theme, PlayerHeads heads) {
        this.session = Objects.requireNonNull(session, "session");
        this.viewer = viewer;
        this.theme = Objects.requireNonNull(theme, "theme");
        this.heads = Objects.requireNonNull(heads, "heads");
        this.measurer = new TextMeasurer(theme);
    }

    public Player viewer() {
        return viewer;
    }

    public CompiledTheme theme() {
        return theme;
    }

    public PlayerHeads heads() {
        return heads;
    }

    /**
     * Width available to content, in pixels.
     * @return the content width
     */
    public int contentWidth() {
        return theme.theme().contentWidth();
    }

    /**
     * Measures text as the client will lay it out.
     * @param text the text
     * @return the advance in pixels
     */
    public int measure(Component text) {
        return measurer.width(text);
    }

    /**
     * Resolves a text value for the viewer: a component, MiniMessage string, message, or other value.
     * @param value the value
     * @return the component
     */
    public Component component(Object value) {
        return CanvasText.resolve(viewer, value);
    }

    /**
     * Resolves a text value that may be absent.
     * @param value the value, or {@code null}
     * @return the component, or {@code null}
     */
    public Component nullableComponent(Object value) {
        return CanvasText.resolveNullable(viewer, value);
    }

    /**
     * Creates a click event bound to this render, for custom components.
     * @param action runs when clicked
     * @return the click event
     */
    public ClickEvent action(Runnable action) {
        return session.action(action);
    }

    /**
     * Creates the click event for an element's action. Internal; used by the canvas components.
     * @param action the action
     * @return the click event
     */
    public ClickEvent click(CanvasAction action) {
        return action.isLink() ? ClickEvent.openUrl(action.url()) : session.action(action.run());
    }

    /**
     * Adds one centred line of MiniMessage text.
     * @param miniMessage the text
     * @return this canvas
     */
    public DialogCanvas text(String miniMessage) {
        return text(component(miniMessage), DialogAlign.CENTER);
    }

    public DialogCanvas text(Component text) {
        return text(text, DialogAlign.CENTER);
    }

    public DialogCanvas text(String miniMessage, DialogAlign align) {
        return text(component(miniMessage), align);
    }

    /**
     * Adds one line of text, shortened with an ellipsis when it is wider than the content area.
     * @param text the text
     * @param align the alignment
     * @return this canvas
     */
    public DialogCanvas text(Component text, DialogAlign align) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(align, "align");
        return block(CanvasBlocks.text(List.of(text), align));
    }

    public DialogCanvas paragraph(String miniMessage) {
        return paragraph(component(miniMessage), DialogAlign.CENTER);
    }

    public DialogCanvas paragraph(Component text) {
        return paragraph(text, DialogAlign.CENTER);
    }

    /**
     * Adds text wrapped at spaces and line breaks to the content width, keeping every part's style.
     * @param text the text
     * @param align the alignment
     * @return this canvas
     */
    public DialogCanvas paragraph(Component text, DialogAlign align) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(align, "align");
        return block(CanvasBlocks.text(measurer.wrap(text, contentWidth()), align));
    }

    public DialogCanvas spacer() {
        return spacer(1);
    }

    /**
     * Adds empty lines.
     * @param lines the number of lines
     * @return this canvas
     */
    public DialogCanvas spacer(int lines) {
        if (lines < 1) throw new IllegalArgumentException("Spacer must be at least one line");

        return block(CanvasBlocks.spacer(lines));
    }

    /**
     * Adds the theme divider across the content width.
     * @return this canvas
     */
    public DialogCanvas divider() {
        return block(CanvasBlocks.asset(theme.asset(DialogTheme.DIVIDER), DialogAlign.CENTER));
    }

    /**
     * Adds centred theme artwork.
     * @param asset the asset name
     * @return this canvas
     */
    public DialogCanvas image(String asset) {
        return block(CanvasBlocks.asset(theme.asset(asset), DialogAlign.CENTER));
    }

    public DialogButton button(String label, Runnable action) {
        return button(component(label), action);
    }

    /**
     * Adds a themed button drawn in the body.
     * @param label the label
     * @param action runs when clicked
     * @return the button, for further configuration
     */
    public DialogButton button(Component label, Runnable action) {
        DialogButton button = new DialogButton(this, label, action);
        blocks.add(button);
        return button;
    }

    public DialogButton link(String label, String url) {
        return link(component(label), url);
    }

    /**
     * Adds a themed button that opens a web page in the viewer's client.
     * <p>
     * Java clients ask before opening it; Bedrock forms cannot open pages, so Bedrock players get the
     * address in chat instead.
     * @param label the label
     * @param url an http or https address
     * @return the button, for further configuration
     */
    public DialogButton link(Component label, String url) {
        DialogButton button = new DialogButton(this, label, CanvasAction.openUrl(url));
        blocks.add(button);
        return button;
    }

    public DialogCanvas head(UUID playerId) {
        return head(playerId, 16);
    }

    /**
     * Adds a centred player face.
     * @param playerId the player
     * @param size 8, 16, or 24 pixels
     * @return this canvas
     */
    public DialogCanvas head(UUID playerId, int size) {
        return card(card -> card.head(playerId, size));
    }

    /**
     * Adds a single centred card.
     * @param builder fills the card
     * @return this canvas
     */
    public DialogCanvas card(Consumer<DialogCard> builder) {
        Objects.requireNonNull(builder, "builder");
        DialogCard card = new DialogCard(this);
        builder.accept(card);
        return block(CanvasBlocks.card(card, DialogAlign.CENTER));
    }

    /**
     * Adds a row of tabs.
     * @param values the tab values, in order
     * @param selected the selected value
     * @param <T> the value type
     * @return the tabs, for further configuration
     */
    public <T> DialogTabs<T> tabs(T[] values, T selected) {
        return tabs(Arrays.asList(values), selected);
    }

    /**
     * Adds a row of tabs.
     * @param values the tab values, in order
     * @param selected the selected value
     * @param <T> the value type
     * @return the tabs, for further configuration
     */
    public <T> DialogTabs<T> tabs(Collection<? extends T> values, T selected) {
        DialogTabs<T> tabs = new DialogTabs<>(this, List.copyOf(values), selected);
        blocks.add(tabs);
        return tabs;
    }

    /**
     * Adds rows of cards, each row centred, such as a 1-3-5-7 leaderboard pyramid.
     * @param rows the entries, grouped into rows; see {@link DialogLayouts#rows}
     * @param renderer fills the card for one entry
     * @param <E> the entry type
     * @return the rows, for further configuration
     */
    public <E> DialogRows<E> centeredRows(List<? extends List<? extends E>> rows, BiConsumer<? super DialogCard, ? super E> renderer) {
        DialogRows<E> block = new DialogRows<>(this, rows, renderer);
        blocks.add(block);
        return block;
    }

    public DialogCanvas footerText(String miniMessage) {
        return footerText(component(miniMessage));
    }

    /**
     * Sets a centred line pinned below every other component.
     * @param text the text
     * @return this canvas
     */
    public DialogCanvas footerText(Component text) {
        this.footer = Objects.requireNonNull(text, "text");
        return this;
    }

    public DialogCanvas exitButton(String label, Runnable handler) {
        return exitButton(component(label), null, DEFAULT_EXIT_WIDTH, handler);
    }

    public DialogCanvas exitButton(Component label, Runnable handler) {
        return exitButton(label, null, DEFAULT_EXIT_WIDTH, handler);
    }

    /**
     * Adds the native dialog exit button, which also handles Escape.
     * <p>
     * Without an exit button, Escape closes the screen on the client without telling the server, so the
     * session stays open until it expires, the player leaves, or another canvas dialog replaces it.
     * The session ends and the screen closes before the handler runs, so the handler may open another dialog.
     * @param label the label
     * @param tooltip the hover text, or {@code null}
     * @param width the button width in pixels, from 1 to 1024
     * @param handler runs when the player exits
     * @return this canvas
     */
    public DialogCanvas exitButton(Component label, Component tooltip, int width, Runnable handler) {
        return exitButton(label, tooltip, width, handler, true);
    }

    public DialogCanvas backButton(String label, Runnable handler) {
        return backButton(component(label), null, DEFAULT_EXIT_WIDTH, handler);
    }

    public DialogCanvas backButton(Component label, Runnable handler) {
        return backButton(label, null, DEFAULT_EXIT_WIDTH, handler);
    }

    /**
     * Adds the native dialog exit button as a way back within the same session, such as from a detail
     * page to its overview. Escape and closing a Bedrock form run it too.
     * <p>
     * Unlike {@link #exitButton}, the session stays open: the handler is expected to show another
     * screen, usually with {@link DialogSession#update}. When it shows nothing, the current screen is
     * shown again, because Escape has already closed it on the client.
     * @param label the label
     * @param tooltip the hover text, or {@code null}
     * @param width the button width in pixels, from 1 to 1024
     * @param handler runs when the player goes back
     * @return this canvas
     */
    public DialogCanvas backButton(Component label, Component tooltip, int width, Runnable handler) {
        return exitButton(label, tooltip, width, handler, false);
    }

    private DialogCanvas exitButton(Component label, Component tooltip, int width, Runnable handler, boolean closes) {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(handler, "handler");
        if (width < 1 || width > 1024) throw new IllegalArgumentException("Exit button width must be between 1 and 1024");

        this.exitButton = new ExitButton(label, tooltip, width, handler, closes, session.exitAction(handler, closes));
        return this;
    }

    /**
     * Components added so far, in order. Internal; read by the renderer.
     * @return the blocks
     */
    public List<CanvasBlock> blocks() {
        return List.copyOf(blocks);
    }

    /**
     * Footer line, if set. Internal; read by the renderer.
     * @return the footer, or {@code null}
     */
    public Component footer() {
        return footer;
    }

    /**
     * Native exit button, if set. Internal; read by the renderer.
     * @return the exit button, or {@code null}
     */
    public ExitButton exitButton() {
        return exitButton;
    }

    /**
     * Describes the screen as text lines and buttons, footer included. Internal; read by the renderers
     * for clients without the theme's artwork. The exit button is not part of the outline.
     * @return the outline
     */
    public CanvasOutline outline() {
        CanvasOutline outline = new CanvasOutline();
        for (CanvasBlock block : blocks) {
            block.outline(outline);
        }
        if (footer != null) {
            outline.blank();
            outline.line(footer);
        }

        return outline;
    }

    private DialogCanvas block(CanvasBlock block) {
        blocks.add(block);
        return this;
    }

    /**
     * Native exit button settings.
     * @param label the label
     * @param tooltip the hover text, or {@code null}
     * @param width the width in pixels
     * @param handler runs when the player exits or goes back
     * @param closes whether exiting ends the session, see {@link #backButton}
     * @param action the dialog action for Java clients
     */
    public record ExitButton(Component label, Component tooltip, int width, Runnable handler, boolean closes, DialogAction action) {
    }
}
