package gg.moonrise.engine.paper.readiness;

import gg.moonrise.engine.paper.scheduler.Scheduler;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import gg.moonrise.engine.state.ServiceReadiness;
import gg.moonrise.engine.state.ServiceReadiness.Enforcement;
import gg.moonrise.engine.state.ServiceReadiness.Failure;
import gg.moonrise.engine.state.ServiceReadiness.FailureAction;
import gg.moonrise.engine.state.ServiceReadiness.ServiceState;
import gg.moonrise.engine.state.ServiceReadiness.Snapshot;
import gg.moonrise.engine.state.ServiceReadiness.Status;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Installs the Paper admission guard for a service readiness contract and applies the
 * controller's plugin-disable or server-stop decision on the server thread.
 *
 * <p>The supplied denial text must be available without loading any operator config. A plugin
 * receives this adapter from {@code PaperPlugin} before enable-time services run. It must gate its own
 * commands and actions on the matching service state, including work by players already online.</p>
 */
public final class PaperServiceReadiness implements Listener, AutoCloseable {

    private final Plugin plugin;
    private final ServiceReadiness readiness;
    private final Component unavailableMessage;
    private final Consumer<Runnable> serverThread;
    private final Runnable stopServer;
    private final Consumer<Plugin> disablePlugin;
    private final ConcurrentMap<String, Failure> loggedPassiveFailures = new ConcurrentHashMap<>();
    private final AtomicBoolean disableQueued = new AtomicBoolean();
    private final AtomicBoolean stopQueued = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile ServiceReadiness.Subscription subscription;
    private volatile ScheduledTask startupDeadline;

    private PaperServiceReadiness(
            Plugin plugin,
            ServiceReadiness readiness,
            Component unavailableMessage,
            Consumer<Runnable> serverThread,
            Runnable stopServer,
            Consumer<Plugin> disablePlugin
    ) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.readiness = Objects.requireNonNull(readiness, "readiness");
        this.unavailableMessage = Objects.requireNonNull(unavailableMessage, "unavailableMessage");
        this.serverThread = Objects.requireNonNull(serverThread, "serverThread");
        this.stopServer = Objects.requireNonNull(stopServer, "stopServer");
        this.disablePlugin = Objects.requireNonNull(disablePlugin, "disablePlugin");
    }

    /**
     * Registers the guard immediately and binds failure actions. {@code PaperPlugin} calls this
     * before enable-time service startup when it finds a readiness bean.
     * @param plugin the owning Paper plugin
     * @param readiness the fixed service contract
     * @param unavailableMessage emergency text independent of operator configuration
     * @param startupTimeout the positive maximum time services may remain pending
     * @return the installed binding, which can be closed during plugin disable
     */
    public static PaperServiceReadiness install(
            Plugin plugin,
            ServiceReadiness readiness,
            Component unavailableMessage,
            Duration startupTimeout
    ) {
        Objects.requireNonNull(startupTimeout, "startupTimeout");
        if (startupTimeout.isZero() || startupTimeout.isNegative()) {
            throw new IllegalArgumentException("startupTimeout must be positive");
        }
        PaperServiceReadiness installed = new PaperServiceReadiness(
                plugin,
                readiness,
                unavailableMessage,
                runnable -> {
                    if (Scheduler.sync().run(task -> runnable.run()) == null) {
                        throw new IllegalStateException("The server rejected a service readiness action");
                    }
                },
                Bukkit::shutdown,
                candidate -> Bukkit.getPluginManager().disablePlugin(candidate)
        );
        try {
            Bukkit.getPluginManager().registerEvents(installed, plugin);
            installed.attach();
            installed.startupDeadline = Scheduler.sync().runDelayed(task -> installed.expirePending(), startupTimeout);
            if (installed.startupDeadline == null) {
                throw new IllegalStateException("The server rejected the service readiness startup deadline");
            }
            return installed;
        } catch (RuntimeException exception) {
            try {
                installed.close();
            } catch (RuntimeException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            boolean protectsConnections = readiness.snapshot().services().values().stream()
                    .anyMatch(state -> state.action() == FailureAction.BLOCK_CONNECTIONS
                            || state.action() == FailureAction.STOP_SERVER);
            plugin.getLogger().log(Level.SEVERE,
                    protectsConnections
                            ? "Could not install service readiness protection; requesting server shutdown"
                            : "Could not install service readiness protection; disabling plugin",
                    exception);
            try {
                if (protectsConnections) {
                    Bukkit.shutdown();
                } else {
                    Bukkit.getPluginManager().disablePlugin(plugin);
                }
            } catch (RuntimeException fallbackFailure) {
                exception.addSuppressed(fallbackFailure);
            }
            throw exception;
        }
    }

    private void attach() {
        subscription = readiness.observe(this::enforce);
        enforce(readiness.snapshot());
    }

    private void expirePending() {
        if (!closed.get()) {
            readiness.expirePending(new Failure(
                    "startup deadline exceeded",
                    new TimeoutException("A required service did not publish a usable state before its deadline")
            ));
        }
    }

    /**
     * Rejects joins during pending, blocked, or stopping critical service states.
     * @param event the asynchronous pre-login event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        Objects.requireNonNull(event, "event");
        if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED
                && readiness.snapshot().blocksConnections()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, unavailableMessage);
        }
    }

    /**
     * Closes the race between asynchronous pre-login and the final synchronous login check.
     * @param event the final login event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onValidateLogin(PlayerConnectionValidateLoginEvent event) {
        Objects.requireNonNull(event, "event");
        if (event.isAllowed() && readiness.snapshot().blocksConnections()) {
            event.kickMessage(unavailableMessage);
        }
    }

    /**
     * Releases the observer when Paper disables the owning plugin.
     * @param event the plugin-disable event
     */
    @EventHandler
    public void onPluginDisable(PluginDisableEvent event) {
        if (event.getPlugin() == plugin) {
            close();
        }
    }

    private void enforce(Snapshot ignored) {
        if (closed.get()) {
            return;
        }
        Snapshot latest = readiness.snapshot();
        logPassiveFailures(latest);
        switch (latest.enforcement()) {
            case STOP_SERVER -> queueAction(stopQueued, Enforcement.STOP_SERVER, stopServer);
            case DISABLE_PLUGIN -> queueAction(disableQueued, Enforcement.DISABLE_PLUGIN,
                    () -> disablePlugin.accept(plugin));
            case NONE, BLOCK_CONNECTIONS -> {
            }
        }
    }

    private void logPassiveFailures(Snapshot snapshot) {
        for (Map.Entry<String, ServiceState> entry : snapshot.services().entrySet()) {
            ServiceState state = entry.getValue();
            if ((state.action() != FailureAction.BLOCK_CONNECTIONS
                    && state.action() != FailureAction.DISABLE_FEATURE)
                    || state.status() != Status.UNAVAILABLE) {
                loggedPassiveFailures.remove(entry.getKey());
                continue;
            }
            Failure failure = state.failure().orElseThrow();
            Failure previous = loggedPassiveFailures.put(entry.getKey(), failure);
            if (!failure.equals(previous)) {
                plugin.getLogger().log(Level.SEVERE,
                        "Service " + entry.getKey() + " is unavailable (" + failure.reason()
                                + "); " + (state.action() == FailureAction.BLOCK_CONNECTIONS
                                ? "blocking connections" : "feature disabled"),
                        failure.cause());
            }
        }
    }

    private void queueAction(AtomicBoolean queued, Enforcement expected, Runnable action) {
        if (!queued.compareAndSet(false, true)) {
            return;
        }
        try {
            serverThread.accept(() -> {
                boolean applied = false;
                try {
                    Snapshot latest = readiness.snapshot();
                    if (!plugin.isEnabled() || closed.get() || latest.enforcement() != expected) {
                        return;
                    }
                    reportFailure(latest, expected);
                    action.run();
                    applied = true;
                } finally {
                    if (!applied) {
                        queued.set(false);
                    }
                }
            });
        } catch (RuntimeException exception) {
            queued.set(false);
            plugin.getLogger().log(Level.SEVERE, "Could not schedule service readiness action " + expected, exception);
            throw exception;
        }
    }

    private void reportFailure(Snapshot snapshot, Enforcement action) {
        FailureAction source = action == Enforcement.STOP_SERVER
                ? FailureAction.STOP_SERVER
                : FailureAction.DISABLE_PLUGIN;
        for (Map.Entry<String, ServiceState> entry : snapshot.services().entrySet()) {
            ServiceState state = entry.getValue();
            if (state.action() == source && state.status() == Status.UNAVAILABLE) {
                Failure failure = state.failure().orElseThrow();
                plugin.getLogger().log(Level.SEVERE,
                        "Service " + entry.getKey() + " is unavailable (" + failure.reason()
                                + "); applying " + action,
                        failure.cause());
                return;
            }
        }
    }

    /** Unregisters listeners, releases the observer, and stops the readiness controller. */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        ServiceReadiness.Subscription attached = subscription;
        if (attached != null) {
            attached.close();
        }
        ScheduledTask deadline = startupDeadline;
        if (deadline != null) {
            deadline.cancel();
        }
        readiness.stop();
        HandlerList.unregisterAll(this);
    }

    static PaperServiceReadiness forTesting(
            Plugin plugin,
            ServiceReadiness readiness,
            Component unavailableMessage,
            Consumer<Runnable> serverThread,
            Runnable stopServer,
            Consumer<Plugin> disablePlugin
    ) {
        PaperServiceReadiness testBinding = new PaperServiceReadiness(
                plugin, readiness, unavailableMessage, serverThread, stopServer, disablePlugin
        );
        testBinding.attach();
        return testBinding;
    }
}
