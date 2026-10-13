package gg.moonrise.engine.paper.dialog.canvas;

import gg.moonrise.engine.paper.dialog.canvas.internal.DialogCanvasListener;
import gg.moonrise.engine.paper.dialog.canvas.internal.DialogClientSupport;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Services shared by every canvas dialog of a plugin: threading, open sessions, pack status, face cache,
 * and Bedrock forms.
 * <p>
 * {@code PaperPlugin} registers the shared runtime's listener on enable, so plugins built on the engine
 * need no setup. Tests and custom bootstraps can {@link #install(DialogCanvasRuntime) install} their own.
 */
public final class DialogCanvasRuntime {

    private static final long SWEEP_SECONDS = 30;
    private static final Function<UUID, String> DEFAULT_BEDROCK_HEADS = playerId -> "https://mc-heads.net/avatar/" + playerId + "/64";

    private static volatile DialogCanvasRuntime shared;

    private final DialogExecutor executor;
    private final DialogSessions sessions = new DialogSessions();
    private final ResourcePackTracker packs = new ResourcePackTracker();
    private final PlayerHeads heads;
    private final DialogClientSupport clients = new DialogClientSupport();
    private final BedrockForms bedrock;
    private final AtomicBoolean registered = new AtomicBoolean();
    private volatile Function<UUID, String> bedrockHeads = DEFAULT_BEDROCK_HEADS;

    public DialogCanvasRuntime(DialogExecutor executor) {
        this(executor, BedrockForms.automatic());
    }

    public DialogCanvasRuntime(DialogExecutor executor, BedrockForms bedrock) {
        this.executor = Objects.requireNonNull(executor, "executor");
        this.bedrock = Objects.requireNonNull(bedrock, "bedrock");
        this.heads = new PlayerHeads(executor);
    }

    /**
     * Returns the shared runtime, creating a scheduler-backed one on first use.
     * @return the runtime
     */
    public static DialogCanvasRuntime shared() {
        DialogCanvasRuntime runtime = shared;
        if (runtime != null) return runtime;

        synchronized (DialogCanvasRuntime.class) {
            if (shared == null) shared = new DialogCanvasRuntime(DialogExecutor.scheduler());
            return shared;
        }
    }

    /**
     * Replaces the shared runtime.
     * @param runtime the runtime
     */
    public static void install(DialogCanvasRuntime runtime) {
        shared = Objects.requireNonNull(runtime, "runtime");
    }

    /**
     * Registers the listener that tracks pack status and ends sessions when players leave, and a
     * task that closes expired sessions every 30 seconds. Idempotent.
     * @param plugin the owning plugin
     */
    public void register(Plugin plugin) {
        Objects.requireNonNull(plugin, "plugin");
        if (!registered.compareAndSet(false, true)) return;

        plugin.getServer().getPluginManager().registerEvents(new DialogCanvasListener(this), plugin);
        plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, task -> sessions.sweep(), SWEEP_SECONDS, SWEEP_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Ends every session so no callback keeps running plugin code after disable. Called by {@code PaperPlugin}.
     */
    public void shutdown() {
        sessions.disposeAll(DialogCloseReason.DISABLED);
    }

    public DialogExecutor executor() {
        return executor;
    }

    public DialogSessions sessions() {
        return sessions;
    }

    public ResourcePackTracker packs() {
        return packs;
    }

    public PlayerHeads heads() {
        return heads;
    }

    public BedrockForms bedrock() {
        return bedrock;
    }

    /**
     * Address of the face image shown on Bedrock form buttons for a player.
     * @param playerId the player
     * @return a web address, or {@code null} for no image
     */
    public String bedrockHeadImage(UUID playerId) {
        return bedrockHeads.apply(playerId);
    }

    /**
     * Sets where Bedrock forms load player faces from. The default is {@code https://mc-heads.net/avatar/<uuid>/64}.
     * @param images returns a web address for a player, or {@code null} for no image
     */
    public void bedrockHeadImages(Function<UUID, String> images) {
        this.bedrockHeads = Objects.requireNonNull(images, "images");
    }

    /**
     * Checks whether a Java client is too old to show dialogs.
     * @param player the player
     * @return the reason it cannot, or empty when it can
     */
    public Optional<DialogUnavailableReason> clientIssue(Player player) {
        return clients.check(player);
    }
}
