package gg.moonrise.engine.paper.dialog.canvas;

import gg.moonrise.engine.message.Message;
import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasText;
import gg.moonrise.engine.paper.dialog.canvas.internal.NativeDialogRenderer;
import gg.moonrise.engine.paper.dialog.canvas.theme.DialogTheme;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * A reusable, themed graphical dialog screen.
 * <p>
 * Subclasses describe a screen from data in {@link #render}; the engine handles font glyphs, spacing,
 * click callbacks, per-player sessions, and stale asynchronous results. One menu instance can serve any
 * number of players at once, each through their own {@link DialogSession}.
 * <p>
 * The same {@link #render} code serves every client. Java players with the theme's pack see the themed
 * artwork, Java players without it a vanilla dialog, and Bedrock players a Floodgate form; see
 * {@link DialogPresentation} and the {@link #nativeFallback} and {@link #bedrockForms} hooks.
 * <pre>{@code
 * public final class ProfileMenu extends DialogMenu<Profile> {
 *
 *     public ProfileMenu(DialogTheme theme) {
 *         super("<aqua>Profile", theme);
 *     }
 *
 *     @Override
 *     protected void render(DialogSession<Profile> session, DialogCanvas ui) {
 *         ui.head(session.playerId(), 16);
 *         ui.text(Component.text(session.data().displayName()));
 *         ui.exitButton("<gold>Back", session::close);
 *     }
 * }
 * }</pre>
 * @param <S> the type of data each screen is rendered from
 */
public abstract class DialogMenu<S> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DialogMenu.class);
    private static final Duration DEFAULT_SESSION_TIMEOUT = Duration.ofMinutes(10);
    private static final int DEFAULT_NATIVE_COLUMNS = 2;
    private static final int DEFAULT_NATIVE_BUTTON_WIDTH = 150;

    private final Object title;
    private final DialogTheme theme;

    /**
     * Creates a menu with a MiniMessage title, resolved per viewer.
     * @param title the title
     * @param theme the theme
     */
    protected DialogMenu(String title, DialogTheme theme) {
        this((Object) Objects.requireNonNull(title, "title"), theme);
    }

    protected DialogMenu(Component title, DialogTheme theme) {
        this((Object) Objects.requireNonNull(title, "title"), theme);
    }

    protected DialogMenu(Message title, DialogTheme theme) {
        this((Object) Objects.requireNonNull(title, "title"), theme);
    }

    private DialogMenu(Object title, DialogTheme theme) {
        this.title = title;
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    /**
     * Describes the screen for the session's current data.
     * <p>
     * Runs on the player's thread for every render. Use only data already in the session; fetch from
     * databases or the network in {@link DialogSession#load}.
     * @param session the viewer's session
     * @param ui the canvas to describe the screen on
     */
    protected abstract void render(DialogSession<S> session, DialogCanvas ui);

    /**
     * Opens this menu for a player, in the presentation their client supports.
     * <p>
     * When the player cannot see this menu at all, {@link #onUnavailable} runs instead and the returned
     * session is already closed. Opening replaces the player's previous canvas session, if any.
     * @param player the viewer
     * @param data the initial data
     * @return the session
     */
    public DialogSession<S> open(Player player, S data) {
        Objects.requireNonNull(player, "player");
        DialogCanvasRuntime runtime = runtime();

        Presentation choice = presentation(runtime, player);
        if (choice.unavailable() != null) {
            onUnavailable(player, data, choice.unavailable());
            return new DialogSession<>(this, runtime, player.getUniqueId(), data, false);
        }

        DialogSession<S> session = new DialogSession<>(this, runtime, player.getUniqueId(), data, true, choice.presentation());
        runtime.sessions().register(session);
        session.refresh();
        return session;
    }

    public DialogTheme theme() {
        return theme;
    }

    /**
     * Resolves the dialog title for a render.
     * @param session the viewer's session
     * @return the title
     */
    protected Component title(DialogSession<S> session) {
        return CanvasText.resolve(session.player().orElse(null), title);
    }

    /**
     * How long a session stays usable after its last render. Also bounds click callback lifetime.
     * @return the timeout
     */
    protected Duration sessionTimeout() {
        return DEFAULT_SESSION_TIMEOUT;
    }

    /**
     * Whether the viewer must have applied a pack with this theme before the menu opens.
     * @return {@code true} by default
     */
    protected boolean requiresResourcePack() {
        return true;
    }

    /**
     * Pack ids that contain this menu's theme. Override when the theme is merged into a server pack
     * sent under a different, fixed id.
     * @return accepted pack ids
     */
    protected Collection<UUID> acceptedResourcePacks() {
        return Set.of(theme.compiled().packId());
    }

    /**
     * Whether a Java player has applied a pack containing this menu's theme. By default, any of
     * {@link #acceptedResourcePacks()}.
     * <p>
     * When the theme is merged into a pack whose id changes with every build, such as one Nexo
     * generates, override this with {@code runtime().packs().isAnyLoaded(player)}.
     * @param player the player
     * @return whether the themed artwork can be shown
     */
    protected boolean hasResourcePack(Player player) {
        return runtime().packs().isLoaded(player.getUniqueId(), acceptedResourcePacks());
    }

    /**
     * Whether Java players without the theme's pack get a vanilla dialog of the same screen instead of
     * {@link #onUnavailable}.
     * @return {@code true} by default
     */
    protected boolean nativeFallback() {
        return true;
    }

    /**
     * Whether Bedrock players get a form of the same screen instead of {@link #onUnavailable}.
     * @return {@code true} by default
     */
    protected boolean bedrockForms() {
        return true;
    }

    /**
     * The Bedrock texture a form button shows for a theme asset, such as a card's icon. None by default.
     * <p>
     * Return a path from {@link gg.moonrise.engine.paper.dialog.canvas.theme.BedrockPackExport#texturePath}
     * once that pack is installed where Geyser loads packs; a path to a texture the client lacks shows
     * an empty image. Vanilla Bedrock paths, such as {@code textures/items/diamond}, work without a pack.
     * Internal callers: the session, on the player's thread.
     * @param asset the theme asset name
     * @return the texture path without {@code .png}, or {@code null} for no image
     */
    public String bedrockImage(String asset) {
        return null;
    }

    /**
     * Button columns of the vanilla dialog shown without the theme's pack.
     * @return the columns, 2 by default
     */
    protected int nativeColumns() {
        return DEFAULT_NATIVE_COLUMNS;
    }

    /**
     * Width of every button of the vanilla dialog shown without the theme's pack.
     * @return the width in pixels, 150 by default
     */
    protected int nativeButtonWidth() {
        return DEFAULT_NATIVE_BUTTON_WIDTH;
    }

    /**
     * Width of the text of the vanilla dialog shown without the theme's pack.
     * @return the width in pixels, the theme width by default
     */
    protected int nativeBodyWidth() {
        return Math.min(1024, theme.width());
    }

    /**
     * Sizes of the vanilla dialog. Internal; read by the session.
     * @return the layout
     */
    public NativeDialogRenderer.Layout nativeLayout() {
        return new NativeDialogRenderer.Layout(nativeColumns(), nativeButtonWidth(), nativeBodyWidth());
    }

    /**
     * Runs instead of opening when the player cannot see this menu. Sends the reason's default message.
     * @param player the player
     * @param data the data the menu would have opened with
     * @param reason why the menu is unavailable
     */
    protected void onUnavailable(Player player, S data, DialogUnavailableReason reason) {
        player.sendRichMessage(reason.defaultMessage());
    }

    /**
     * Runs once when a session ends, on the player's thread while they are online.
     * @param session the session
     * @param reason why it ended
     */
    protected void onClose(DialogSession<S> session, DialogCloseReason reason) {
    }

    /**
     * Runs when the newest {@link DialogSession#load} fails, on the player's thread while they are online.
     * Logs the error and keeps the current screen; Bedrock players, whose form the click consumed, are
     * sent it again afterwards.
     * @param session the session
     * @param error the failure
     */
    protected void onLoadFailure(DialogSession<S> session, Throwable error) {
        LOGGER.error("Dialog menu {} failed to load data for {}", getClass().getName(), session.playerId(), error);
    }

    /**
     * Runtime providing threading, sessions, pack status, and faces.
     * @return the shared runtime by default
     */
    protected DialogCanvasRuntime runtime() {
        return DialogCanvasRuntime.shared();
    }

    private Presentation presentation(DialogCanvasRuntime runtime, Player player) {
        if (runtime.bedrock().isBedrock(player)) {
            return bedrockForms()
                    ? Presentation.of(DialogPresentation.BEDROCK)
                    : Presentation.unavailable(DialogUnavailableReason.BEDROCK_CLIENT);
        }

        Optional<DialogUnavailableReason> client = runtime.clientIssue(player);
        if (client.isPresent()) return Presentation.unavailable(client.get());

        if (!requiresResourcePack() || hasResourcePack(player)) return Presentation.of(DialogPresentation.THEMED);
        if (nativeFallback()) return Presentation.of(DialogPresentation.NATIVE);

        return Presentation.unavailable(DialogUnavailableReason.RESOURCE_PACK_NOT_LOADED);
    }

    /**
     * The presentation chosen for a viewer, or why there is none.
     */
    private record Presentation(DialogPresentation presentation, DialogUnavailableReason unavailable) {

        private static Presentation of(DialogPresentation presentation) {
            return new Presentation(presentation, null);
        }

        private static Presentation unavailable(DialogUnavailableReason reason) {
            return new Presentation(null, reason);
        }
    }
}
