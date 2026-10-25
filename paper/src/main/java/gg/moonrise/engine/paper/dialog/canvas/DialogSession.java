package gg.moonrise.engine.paper.dialog.canvas;

import gg.moonrise.engine.paper.dialog.canvas.internal.BedrockFormRenderer;
import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasAction;
import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasOverflowException;
import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasRenderer;
import gg.moonrise.engine.paper.dialog.canvas.internal.NativeDialogRenderer;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * One player's view of a {@link DialogMenu}: its data, its registered actions, and its lifecycle.
 * <p>
 * Every render increments the session's generation. Click actions remember the generation they were
 * rendered in and do nothing once the screen has been replaced or the session has ended. Every
 * {@link #load} gets a ticket; a result is only applied when no newer load or {@link #update} happened
 * since, so a slow query can never overwrite a newer screen.
 * <p>
 * The session renders in the {@link DialogPresentation} chosen when the menu opened. Bedrock forms are
 * consumed by a response, so after a Bedrock click that shows no new screen the current one is sent again.
 * @param <S> the type of data the menu renders
 */
public final class DialogSession<S> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DialogSession.class);
    private static final Duration EXIT_GRACE = Duration.ofMinutes(10);
    private static final long NO_FORM = -1;

    private final DialogMenu<S> menu;
    private final DialogCanvasRuntime runtime;
    private final UUID playerId;
    private final DialogPresentation presentation;
    private final AtomicBoolean open;
    private final AtomicLong generation = new AtomicLong();
    private final AtomicLong loadTicket = new AtomicLong();
    private final AtomicLong settledTicket = new AtomicLong();
    // The render whose Bedrock form is on the client unanswered, or NO_FORM
    private final AtomicLong shownForm = new AtomicLong(NO_FORM);
    private final AtomicBoolean overflowWarned = new AtomicBoolean();
    private volatile S data;
    private volatile Dialog currentDialog;
    private volatile BedrockForm currentForm;
    private volatile long expiresAt;

    public DialogSession(DialogMenu<S> menu, DialogCanvasRuntime runtime, UUID playerId, S data, boolean open) {
        this(menu, runtime, playerId, data, open, DialogPresentation.THEMED);
    }

    public DialogSession(DialogMenu<S> menu, DialogCanvasRuntime runtime, UUID playerId, S data, boolean open, DialogPresentation presentation) {
        this.menu = Objects.requireNonNull(menu, "menu");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.presentation = Objects.requireNonNull(presentation, "presentation");
        this.data = data;
        this.open = new AtomicBoolean(open);
        this.expiresAt = System.currentTimeMillis() + menu.sessionTimeout().toMillis();
    }

    public DialogMenu<S> menu() {
        return menu;
    }

    /**
     * Runtime the session runs in, for example to preload faces with {@link PlayerHeads#preload}.
     * @return the runtime
     */
    public DialogCanvasRuntime runtime() {
        return runtime;
    }

    public UUID playerId() {
        return playerId;
    }

    /**
     * How this session is shown: themed artwork, a vanilla dialog, or a Bedrock form.
     * @return the presentation
     */
    public DialogPresentation presentation() {
        return presentation;
    }

    /**
     * Returns the viewer if they are online.
     * @return the player
     */
    public Optional<Player> player() {
        return Optional.ofNullable(Bukkit.getPlayer(playerId));
    }

    /**
     * Returns the data the current screen was, or the next screen will be, rendered from.
     * @return the data
     */
    public S data() {
        return data;
    }

    /**
     * Number of renders so far; actions from older renders are inert.
     * @return the render generation
     */
    public long generation() {
        return generation.get();
    }

    /**
     * The dialog most recently shown to a Java player.
     * @return the dialog, or {@code null} before the first render and for Bedrock forms
     */
    public Dialog currentDialog() {
        return currentDialog;
    }

    /**
     * The form most recently sent to a Bedrock player.
     * @return the form, or {@code null} before the first render and for Java dialogs
     */
    public BedrockForm currentForm() {
        return currentForm;
    }

    /**
     * Returns whether the session is still live: not closed, replaced, or expired.
     * @return whether the session is open
     */
    public boolean isOpen() {
        if (!open.get()) return false;
        if (System.currentTimeMillis() > expiresAt) {
            close(DialogCloseReason.EXPIRED);
            return false;
        }

        return true;
    }

    /**
     * Returns whether a {@link #load} is still outstanding.
     * @return whether a load is running
     */
    public boolean isLoading() {
        return settledTicket.get() < loadTicket.get();
    }

    /**
     * Replaces the data and shows the new screen. Cancels any outstanding load.
     * @param data the new data
     */
    public void update(S data) {
        synchronized (this) {
            settle(loadTicket.incrementAndGet());
            this.data = data;
        }
        refresh();
    }

    /**
     * Renders the current data and shows it as a replacement dialog.
     * <p>
     * Native inputs, focus, and scroll position are not preserved.
     */
    public void refresh() {
        Player player = player().orElse(null);
        if (player == null) {
            dispose(DialogCloseReason.QUIT);
            return;
        }

        runtime.executor().player(player, this::render, () -> dispose(DialogCloseReason.QUIT));
    }

    /**
     * Loads new data off the server thread, then shows it if it is still the newest request.
     * @param loader produces the data; runs on {@link DialogExecutor#async}
     * @return completes with {@code true} when the result was shown, {@code false} when it was discarded
     */
    public CompletableFuture<Boolean> load(Supplier<? extends S> loader) {
        Objects.requireNonNull(loader, "loader");
        return loadAsync(() -> {
            CompletableFuture<S> future = new CompletableFuture<>();
            runtime.executor().async(() -> {
                try {
                    future.complete(loader.get());
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            return future;
        });
    }

    /**
     * Loads new data from an asynchronous source, then shows it if it is still the newest request.
     * @param loader starts the request; called on the current thread
     * @return completes with {@code true} when the result was shown, {@code false} when it was discarded
     */
    public CompletableFuture<Boolean> loadAsync(Supplier<? extends CompletionStage<? extends S>> loader) {
        Objects.requireNonNull(loader, "loader");
        long ticket = loadTicket.incrementAndGet();
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        CompletionStage<? extends S> stage;
        try {
            stage = Objects.requireNonNull(loader.get(), "loader returned null");
        } catch (Throwable t) {
            failLoad(ticket, t, result);
            return result;
        }

        stage.whenComplete((value, error) -> {
            if (error != null) {
                failLoad(ticket, error, result);
                return;
            }

            Player player = player().orElse(null);
            if (!isCurrent(ticket) || player == null) {
                settle(ticket);
                result.complete(false);
                return;
            }

            try {
                runtime.executor().player(player, () -> apply(ticket, value, result), () -> {
                    settle(ticket);
                    result.complete(false);
                });
            } catch (Throwable t) {
                settle(ticket);
                result.completeExceptionally(t);
            }
        });

        return result;
    }

    /**
     * Ends the session and closes the player's dialog screen.
     */
    public void close() {
        close(DialogCloseReason.CLOSED);
    }

    /**
     * Ends the session and closes the player's dialog screen.
     * <p>
     * A Bedrock session only closes its own form, and only while it is unanswered.
     * @param reason why the session ends
     */
    public void close(DialogCloseReason reason) {
        if (!dispose(reason) || presentation == DialogPresentation.BEDROCK) return;

        player().ifPresent(player -> runtime.executor().player(player, player::closeDialog, () -> {
        }));
    }

    /**
     * Ends the session without touching the player's screen. Idempotent.
     * @param reason why the session ends
     * @return {@code true} when this call ended the session
     */
    public boolean dispose(DialogCloseReason reason) {
        if (!open.compareAndSet(true, false)) return false;

        runtime.sessions().remove(this);
        Runnable hook = () -> {
            try {
                menu.onClose(this, reason);
            } catch (RuntimeException e) {
                LOGGER.error("Dialog menu {} failed while closing", menu.getClass().getName(), e);
            }
        };
        // Clear this session's form from the client, or it would stay in front of whatever comes next
        if (shownForm.getAndSet(NO_FORM) != NO_FORM && reason != DialogCloseReason.QUIT) {
            Runnable withForm = hook;
            hook = () -> {
                player().ifPresent(runtime.bedrock()::close);
                withForm.run();
            };
        }
        // A disabling plugin can no longer schedule tasks
        if (reason == DialogCloseReason.DISABLED) {
            hook.run();
        } else {
            onPlayerThread(hook);
        }

        return true;
    }

    /**
     * Creates a click event that runs an action for this session's current render.
     * <p>
     * The action runs on the player's thread, and only while this session is open and the screen it
     * was rendered on has not been replaced.
     * @param action the action
     * @return the click event to attach to a component
     */
    public ClickEvent action(Runnable action) {
        Objects.requireNonNull(action, "action");
        long rendered = generation.get();
        return ClickEvent.callback(audience -> handle(audience, rendered, action, Exit.NONE), callbackOptions(menu.sessionTimeout()));
    }

    /**
     * Creates the dialog action behind a vanilla dialog button, for this session's current render.
     * @param action the action
     * @return the dialog action
     */
    public DialogAction dialogAction(Runnable action) {
        Objects.requireNonNull(action, "action");
        long rendered = generation.get();
        return DialogAction.customClick((view, audience) -> handle(audience, rendered, action, Exit.NONE),
                callbackOptions(menu.sessionTimeout()));
    }

    /**
     * Creates the dialog action behind the native exit button and the Escape key.
     * <p>
     * The session ends and the screen closes before the handler runs, so the handler may open any other
     * dialog or menu. The exit callback outlives the session, so an expired screen can still be left.
     * @param handler the exit handler
     * @return the dialog action
     */
    public DialogAction exitAction(Runnable handler) {
        return exitAction(handler, true);
    }

    /**
     * Creates the dialog action behind the native exit button and the Escape key.
     * @param handler the exit handler
     * @param closes {@code true} to end the session before the handler runs, {@code false} to keep it
     *               open as a way back, see {@link DialogCanvas#backButton}
     * @return the dialog action
     */
    public DialogAction exitAction(Runnable handler, boolean closes) {
        Objects.requireNonNull(handler, "handler");
        long rendered = generation.get();
        Exit exit = closes ? Exit.CLOSE : Exit.BACK;
        return DialogAction.customClick((view, audience) -> handle(audience, rendered, handler, exit),
                callbackOptions(menu.sessionTimeout().plus(EXIT_GRACE)));
    }

    private void render() {
        if (!isOpen()) return;

        Player player = player().orElse(null);
        if (player == null) {
            dispose(DialogCloseReason.QUIT);
            return;
        }

        long rendered = generation.incrementAndGet();
        try {
            DialogCanvas canvas = new DialogCanvas(this, player, menu.theme().compiled(), runtime.heads());
            menu.render(this, canvas);
            Component title = menu.title(this);

            expiresAt = System.currentTimeMillis() + menu.sessionTimeout().toMillis();
            switch (presentation) {
                case THEMED -> show(player, themed(canvas, title));
                case NATIVE -> show(player, NativeDialogRenderer.render(canvas, title, menu.nativeLayout(), this::dialogAction));
                case BEDROCK -> send(player, BedrockFormRenderer.render(canvas, title, runtime::bedrockHeadImage, menu::bedrockImage, new FormHandler(rendered, canvas.exitButton())));
            }
        } catch (RuntimeException e) {
            close(DialogCloseReason.FAILED);
            throw e;
        }
    }

    /**
     * Renders the themed screen, or the vanilla one when the content does not fit the theme.
     */
    private Dialog themed(DialogCanvas canvas, Component title) {
        try {
            return CanvasRenderer.render(canvas, title).dialog();
        } catch (CanvasOverflowException e) {
            // Every refresh renders again; warn once per session
            if (overflowWarned.compareAndSet(false, true)) {
                LOGGER.warn("Dialog menu {} showed a vanilla dialog to {}: {}", menu.getClass().getName(), playerId, e.getMessage());
            } else {
                LOGGER.debug("Dialog menu {} showed a vanilla dialog to {}: {}", menu.getClass().getName(), playerId, e.getMessage());
            }
            return NativeDialogRenderer.render(canvas, title, menu.nativeLayout(), this::dialogAction);
        }
    }

    private void show(Player player, Dialog dialog) {
        currentDialog = dialog;
        player.showDialog(dialog);
    }

    private void send(Player player, BedrockForm form) {
        // Forms stack on the client: an unanswered one would stay in front of the new one
        if (shownForm.getAndSet(NO_FORM) != NO_FORM) runtime.bedrock().close(player);

        currentForm = form;
        if (!runtime.bedrock().send(player, form)) throw new IllegalStateException("Floodgate or Geyser did not accept the form for " + player.getName());
        shownForm.set(generation.get());
    }

    private void handle(Audience audience, long rendered, Runnable action, Exit exit) {
        if (!(audience instanceof Player player) || !player.getUniqueId().equals(playerId)) return;

        runtime.executor().player(player, () -> respond(player, rendered, action, exit), () -> dispose(DialogCloseReason.QUIT));
    }

    /**
     * Runs a click on the player's thread.
     */
    private void respond(Player player, long rendered, Runnable action, Exit exit) {
        if (!isOpen() || rendered != generation.get()) {
            if (exit == Exit.NONE) return;

            if (isOpen()) {
                // The player left an older screen of this session; leaving means leaving, going back shows the current one
                if (exit == Exit.CLOSE) close(DialogCloseReason.EXITED);
                else render();
            } else if (presentation != DialogPresentation.BEDROCK && runtime.sessions().active(playerId).isEmpty()) {
                // The exit button must always get the player out of an ended session's screen, but never close a newer one
                player.closeDialog();
            }
            return;
        }

        if (exit == Exit.CLOSE) close(DialogCloseReason.EXITED);
        try {
            action.run();
        } catch (RuntimeException e) {
            LOGGER.error("Dialog menu {} action failed for {}", menu.getClass().getName(), player.getName(), e);
        }

        // Escape and Bedrock responses leave no screen behind; show this one again unless the action replaced it
        boolean screenGone = exit == Exit.BACK || presentation == DialogPresentation.BEDROCK;
        if (screenGone && isOpen() && rendered == generation.get() && !isLoading()) render();
    }

    private void onPlayerThread(Runnable hook) {
        Player player = player().orElse(null);
        if (player == null) {
            hook.run();
            return;
        }

        try {
            runtime.executor().player(player, hook, hook);
        } catch (RuntimeException e) {
            LOGGER.error("Could not schedule dialog hook for {}", playerId, e);
            hook.run();
        }
    }

    private void apply(long ticket, S value, CompletableFuture<Boolean> result) {
        synchronized (this) {
            settle(ticket);
            if (!isCurrent(ticket)) {
                result.complete(false);
                return;
            }

            this.data = value;
        }

        try {
            render();
            result.complete(true);
        } catch (Throwable t) {
            result.completeExceptionally(t);
        }
    }

    private boolean isCurrent(long ticket) {
        return ticket == loadTicket.get() && isOpen();
    }

    private void failLoad(long ticket, Throwable error, CompletableFuture<Boolean> result) {
        settle(ticket);
        if (ticket == loadTicket.get() && open.get()) {
            onPlayerThread(() -> {
                try {
                    menu.onLoadFailure(this, error);
                } catch (RuntimeException e) {
                    LOGGER.error("Dialog menu {} failed while handling a load failure", menu.getClass().getName(), e);
                }
                // The response that started the load consumed the Bedrock form; show the current screen again
                if (presentation == DialogPresentation.BEDROCK && isOpen() && !isLoading()) render();
            });
        }

        result.completeExceptionally(error);
    }

    private void settle(long ticket) {
        settledTicket.accumulateAndGet(ticket, Math::max);
    }

    /**
     * What a click does to the session besides running its action.
     */
    private enum Exit {
        /**
         * An ordinary button.
         */
        NONE,
        /**
         * The exit button: the session ends first.
         */
        CLOSE,
        /**
         * A back button: the session stays open.
         */
        BACK
    }

    /**
     * Receives the responses to one Bedrock form.
     */
    private final class FormHandler implements BedrockFormRenderer.Handler {

        private final long rendered;
        private final DialogCanvas.ExitButton exit;

        private FormHandler(long rendered, DialogCanvas.ExitButton exit) {
            this.rendered = rendered;
            this.exit = exit;
        }

        @Override
        public void click(CanvasAction action) {
            answered();
            onPlayer(player -> {
                if (action == null) {
                    // A face card: there is nothing to do, but the response consumed the form
                    respond(player, rendered, () -> {
                    }, Exit.NONE);
                } else if (action.isLink()) {
                    respond(player, rendered, () -> player.sendMessage(Component.text(action.url(), NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.openUrl(action.url()))), Exit.NONE);
                } else {
                    respond(player, rendered, action.run(), Exit.NONE);
                }
            });
        }

        @Override
        public void exit() {
            answered();
            if (exit != null) onPlayer(player -> respond(player, rendered, exit.handler(), exit.closes() ? Exit.CLOSE : Exit.BACK));
        }

        @Override
        public void close() {
            answered();
            onPlayer(player -> {
                // A replaced form reports a close too; only the current form's close means anything
                if (!isOpen() || rendered != generation.get()) return;

                if (exit != null) {
                    respond(player, rendered, exit.handler(), exit.closes() ? Exit.CLOSE : Exit.BACK);
                } else {
                    dispose(DialogCloseReason.CLOSED);
                }
            });
        }

        /**
         * Every response consumes its form on the client; only this render's form is the one tracked.
         */
        private void answered() {
            shownForm.compareAndSet(rendered, NO_FORM);
        }

        private void onPlayer(Consumer<Player> task) {
            Player player = player().orElse(null);
            if (player == null) {
                dispose(DialogCloseReason.QUIT);
                return;
            }

            runtime.executor().player(player, () -> task.accept(player), () -> dispose(DialogCloseReason.QUIT));
        }
    }

    private static ClickCallback.Options callbackOptions(Duration lifetime) {
        return ClickCallback.Options.builder()
                .uses(ClickCallback.UNLIMITED_USES)
                .lifetime(lifetime)
                .build();
    }
}
