package gg.moonrise.engine.paper.dialog.canvas;

import gg.moonrise.engine.paper.dialog.canvas.theme.DialogTheme;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Builds a one-off themed dialog without declaring a {@link DialogMenu} subclass.
 * <pre>{@code
 * Dialogs.canvas(player)
 *         .theme(theme)
 *         .title("<gold>Rewards")
 *         .render((session, ui) -> {
 *             ui.text("<yellow>Daily reward ready");
 *             ui.button("<green>Claim", () -> claim(player));
 *             ui.exitButton("Close", session::close);
 *         })
 *         .open();
 * }</pre>
 */
public final class CanvasDialogBuilder {

    private final Player player;
    private DialogTheme theme;
    private Object title = Component.empty();
    private boolean requiresResourcePack = true;
    private BiConsumer<DialogSession<Void>, DialogCanvas> renderer;

    public CanvasDialogBuilder(Player player) {
        this.player = Objects.requireNonNull(player, "player");
    }

    public CanvasDialogBuilder theme(DialogTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
        return this;
    }

    public CanvasDialogBuilder title(String title) {
        this.title = Objects.requireNonNull(title, "title");
        return this;
    }

    public CanvasDialogBuilder title(Component title) {
        this.title = Objects.requireNonNull(title, "title");
        return this;
    }

    /**
     * Skips the resource pack check, for servers that deliver the theme pack in a way the tracker cannot see.
     * @param requiresResourcePack whether the theme pack must be loaded
     * @return this builder
     */
    public CanvasDialogBuilder requiresResourcePack(boolean requiresResourcePack) {
        this.requiresResourcePack = requiresResourcePack;
        return this;
    }

    /**
     * Sets how the screen is described; runs on every render.
     * @param renderer describes the screen
     * @return this builder
     */
    public CanvasDialogBuilder render(BiConsumer<DialogSession<Void>, DialogCanvas> renderer) {
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        return this;
    }

    /**
     * Opens the dialog.
     * @return the session
     */
    public DialogSession<Void> open() {
        if (theme == null) throw new IllegalStateException("A canvas dialog needs a theme");
        if (renderer == null) throw new IllegalStateException("A canvas dialog needs a renderer");

        InlineMenu menu = title instanceof String miniMessage
                ? new InlineMenu(miniMessage, theme, requiresResourcePack, renderer)
                : new InlineMenu((Component) title, theme, requiresResourcePack, renderer);
        return menu.open(player, null);
    }

    private static final class InlineMenu extends DialogMenu<Void> {

        private final boolean requiresResourcePack;
        private final BiConsumer<DialogSession<Void>, DialogCanvas> renderer;

        private InlineMenu(String title, DialogTheme theme, boolean requiresResourcePack, BiConsumer<DialogSession<Void>, DialogCanvas> renderer) {
            super(title, theme);
            this.requiresResourcePack = requiresResourcePack;
            this.renderer = renderer;
        }

        private InlineMenu(Component title, DialogTheme theme, boolean requiresResourcePack, BiConsumer<DialogSession<Void>, DialogCanvas> renderer) {
            super(title, theme);
            this.requiresResourcePack = requiresResourcePack;
            this.renderer = renderer;
        }

        @Override
        protected void render(DialogSession<Void> session, DialogCanvas ui) {
            renderer.accept(session, ui);
        }

        @Override
        protected boolean requiresResourcePack() {
            return requiresResourcePack;
        }
    }
}
