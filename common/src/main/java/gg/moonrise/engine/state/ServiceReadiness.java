package gg.moonrise.engine.state;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/**
 * A fixed, thread-safe contract for the services one application needs to operate.
 *
 * <p>Readiness starts pending. A service becomes ready only after its complete usable state has
 * been published. Rejecting a candidate reload records the failure without revoking an active
 * service; losing the active state is a separate, explicit transition. This class performs no I/O,
 * scheduling, logging, or platform action.</p>
 */
public final class ServiceReadiness {

    private final AtomicReference<Snapshot> current;
    private final CopyOnWriteArrayList<Consumer<Snapshot>> observers = new CopyOnWriteArrayList<>();

    private ServiceReadiness(Map<String, FailureAction> definitions) {
        Map<String, ServiceState> initial = new LinkedHashMap<>();
        definitions.forEach((name, action) -> initial.put(name, new ServiceState(
                action, Status.PENDING, Optional.empty(), Optional.empty()
        )));
        this.current = new AtomicReference<>(snapshot(0L, false, initial));
    }

    /**
     * Creates a fixed-registry builder.
     * @return a builder for a fixed service registry
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Reads the current decision without locking or I/O.
     * @return an immutable view of the current decision
     */
    public Snapshot snapshot() {
        return current.get();
    }

    /**
     * Call only after publishing a complete usable state for this service.
     * @param service the registered service name
     * @return the resulting snapshot
     */
    public Snapshot ready(String service) {
        return update(previous -> {
            ServiceState before = previous.service(service);
            if (before.status() == Status.READY && before.candidateFailure().isEmpty()) {
                return previous;
            }
            return replace(previous, service, new ServiceState(
                    before.action(), Status.READY, Optional.empty(), Optional.empty()
            ));
        });
    }

    /**
     * Reports that no usable active state exists. This also applies after a previously ready
     * service loses its active state; it is not the method for a rejected candidate reload.
     * @param service the registered service name
     * @param failure the reason no active state is usable
     * @return the resulting snapshot
     */
    public Snapshot unavailable(String service, Failure failure) {
        Objects.requireNonNull(failure, "failure");
        return update(previous -> {
            ServiceState before = previous.service(service);
            return replace(previous, service, new ServiceState(
                    before.action(), Status.UNAVAILABLE, Optional.of(failure), Optional.empty()
            ));
        });
    }

    /**
     * Marks only still-pending services unavailable after a startup deadline. Completed services
     * and their last-good state are unchanged. The host schedules the one-shot deadline.
     * @param failure the deadline failure to attach to pending services
     * @return the resulting snapshot
     */
    public Snapshot expirePending(Failure failure) {
        Objects.requireNonNull(failure, "failure");
        return update(previous -> {
            Map<String, ServiceState> services = new LinkedHashMap<>(previous.services());
            boolean changed = false;
            for (Map.Entry<String, ServiceState> entry : services.entrySet()) {
                ServiceState state = entry.getValue();
                if (state.status() == Status.PENDING) {
                    entry.setValue(new ServiceState(
                            state.action(), Status.UNAVAILABLE, Optional.of(failure), Optional.empty()
                    ));
                    changed = true;
                }
            }
            return changed ? snapshot(previous.revision() + 1, false, services) : previous;
        });
    }

    /**
     * Keeps a ready service live when a proposed replacement fails before publication.
     * @param service the registered service name
     * @param failure the rejected candidate's failure
     * @return the resulting snapshot
     */
    public Snapshot rejectCandidate(String service, Failure failure) {
        Objects.requireNonNull(failure, "failure");
        return update(previous -> {
            ServiceState before = previous.service(service);
            if (before.status() != Status.READY) {
                throw new IllegalStateException("No active state to retain for service: " + service);
            }
            return replace(previous, service, new ServiceState(
                    before.action(), Status.READY, Optional.empty(), Optional.of(failure)
            ));
        });
    }

    /**
     * Prevents any later transition and closes connection admission while stopping.
     * @return the stopped snapshot
     */
    public Snapshot stop() {
        while (true) {
            Snapshot before = current.get();
            if (before.stopped()) {
                return before;
            }
            Snapshot after = snapshot(before.revision() + 1, true, before.services());
            if (current.compareAndSet(before, after)) {
                notifyObservers(after);
                return after;
            }
        }
    }

    /**
     * Observes transitions on the caller's thread. The callback must stay short and must re-read
     * {@link #snapshot()} before irreversible work because concurrent notifications can arrive out
     * of order. Closing the subscription removes it; no worker or queue is created. This observes
     * future transitions; the caller reads the current snapshot after subscribing.
     * @param observer a short synchronous callback
     * @return the registration to close when no longer needed
     */
    public Subscription observe(Consumer<Snapshot> observer) {
        Objects.requireNonNull(observer, "observer");
        observers.add(observer);
        return () -> observers.remove(observer);
    }

    private Snapshot update(UnaryOperator<Snapshot> change) {
        while (true) {
            Snapshot before = current.get();
            if (before.stopped()) {
                throw new IllegalStateException("Service readiness is stopped");
            }
            Snapshot after = change.apply(before);
            if (after == before) {
                return before;
            }
            if (current.compareAndSet(before, after)) {
                notifyObservers(after);
                return after;
            }
        }
    }

    private Snapshot replace(Snapshot before, String service, ServiceState state) {
        Map<String, ServiceState> services = new LinkedHashMap<>(before.services());
        services.put(service, state);
        return snapshot(before.revision() + 1, false, services);
    }

    private static Snapshot snapshot(long revision, boolean stopped, Map<String, ServiceState> services) {
        Map<String, ServiceState> copy = Collections.unmodifiableMap(new LinkedHashMap<>(services));
        Enforcement enforcement = decide(stopped, copy);
        return new Snapshot(revision, stopped, copy, enforcement);
    }

    private static Enforcement decide(boolean stopped, Map<String, ServiceState> services) {
        if (stopped) {
            return Enforcement.BLOCK_CONNECTIONS;
        }
        if (services.values().stream().anyMatch(state -> state.status() == Status.UNAVAILABLE
                && state.action() == FailureAction.STOP_SERVER)) {
            return Enforcement.STOP_SERVER;
        }
        if (services.values().stream().anyMatch(state -> state.status() != Status.READY
                && (state.action() == FailureAction.BLOCK_CONNECTIONS
                || state.action() == FailureAction.STOP_SERVER))) {
            return Enforcement.BLOCK_CONNECTIONS;
        }
        if (services.values().stream().anyMatch(state -> state.status() == Status.UNAVAILABLE
                && state.action() == FailureAction.DISABLE_PLUGIN)) {
            return Enforcement.DISABLE_PLUGIN;
        }
        return Enforcement.NONE;
    }

    private void notifyObservers(Snapshot snapshot) {
        RuntimeException first = null;
        for (Consumer<Snapshot> observer : observers) {
            try {
                observer.accept(snapshot);
            } catch (RuntimeException exception) {
                if (first == null) {
                    first = exception;
                } else {
                    first.addSuppressed(exception);
                }
            }
        }
        if (first != null) {
            throw first;
        }
    }

    /** The policy assigned to a logical service before any configuration is loaded. */
    public enum FailureAction {
        /** Only calls to this feature are denied. */
        DISABLE_FEATURE,
        /** Disable the owning plugin after a reported failure. */
        DISABLE_PLUGIN,
        /** Keep a guard active and reject new connections. */
        BLOCK_CONNECTIONS,
        /** Block connections and request a graceful server shutdown. */
        STOP_SERVER
    }

    /** The active state of one service. */
    public enum Status {
        /** No complete state has been published yet. */
        PENDING,
        /** A complete usable state is active. */
        READY,
        /** No usable active state exists. */
        UNAVAILABLE
    }

    /** The strongest platform action needed by the current snapshot. */
    public enum Enforcement {
        /** No global platform action; individual features may still be unavailable. */
        NONE,
        /** Disable the plugin on its owning thread. */
        DISABLE_PLUGIN,
        /** Reject new connections while keeping the plugin enabled. */
        BLOCK_CONNECTIONS,
        /** Reject connections and request a graceful server shutdown. */
        STOP_SERVER
    }

    /**
     * One bounded failure record per service. Keep player-facing text outside this type.
     * @param reason an operator-facing context string
     * @param cause the original failure
     */
    public record Failure(String reason, Throwable cause) {
        /** Validates the operator context and original cause. */
        public Failure {
            Objects.requireNonNull(reason, "reason");
            if (reason.isBlank()) {
                throw new IllegalArgumentException("reason must not be blank");
            }
            Objects.requireNonNull(cause, "cause");
        }
    }

    /**
     * The bounded status and last failure for one registered service.
     * @param action the fixed failure policy
     * @param status the active readiness state
     * @param failure the current unavailability, if any
     * @param candidateFailure the most recently rejected replacement, if any
     */
    public record ServiceState(
            FailureAction action,
            Status status,
            Optional<Failure> failure,
            Optional<Failure> candidateFailure
    ) {
        /** Validates the state and its optional failures. */
        public ServiceState {
            Objects.requireNonNull(action, "action");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(failure, "failure");
            Objects.requireNonNull(candidateFailure, "candidateFailure");
            if ((status == Status.UNAVAILABLE) != failure.isPresent()) {
                throw new IllegalArgumentException("An unavailable service must have exactly one active failure");
            }
            if (status != Status.READY && candidateFailure.isPresent()) {
                throw new IllegalArgumentException("Only a ready service can retain a candidate failure");
            }
        }
    }

    /**
     * An immutable view published atomically after each transition.
     * @param revision the monotonically increasing transition number
     * @param stopped whether further transitions are prohibited
     * @param services the fixed service registry and its states
     * @param enforcement the current global platform decision
     */
    public record Snapshot(
            long revision,
            boolean stopped,
            Map<String, ServiceState> services,
            Enforcement enforcement
    ) {
        /** Copies the registry and verifies the supplied decision. */
        public Snapshot {
            Objects.requireNonNull(services, "services");
            Objects.requireNonNull(enforcement, "enforcement");
            services = Collections.unmodifiableMap(new LinkedHashMap<>(services));
            if (enforcement != decide(stopped, services)) {
                throw new IllegalArgumentException("Enforcement must match service states");
            }
        }

        /**
         * Looks up a registered service or rejects an unknown name.
         * @param name the registered service name
         * @return its immutable state
         */
        public ServiceState service(String name) {
            ServiceState state = services.get(Objects.requireNonNull(name, "name"));
            if (state == null) {
                throw new IllegalArgumentException("Unknown service: " + name);
            }
            return state;
        }

        /**
         * Whether this service has a usable active state.
         * @param name the registered service name
         * @return whether an active state is usable
         */
        public boolean isReady(String name) {
            return service(name).status() == Status.READY && !stopped;
        }

        /**
         * Checks admission from a precomputed decision.
         * @return whether the current policy requires rejecting new connections
         */
        public boolean blocksConnections() {
            return enforcement == Enforcement.BLOCK_CONNECTIONS
                    || enforcement == Enforcement.STOP_SERVER;
        }
    }

    /** A removable observer registration. */
    @FunctionalInterface
    public interface Subscription extends AutoCloseable {
        @Override
        void close();
    }

    /** Builds a fixed registry of logical services. */
    public static final class Builder {
        private final Map<String, FailureAction> definitions = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * Registers one logical service and its unavailable-state policy.
         * @param name the stable service name
         * @param action the policy chosen before loading configuration
         * @return this builder
         */
        public Builder service(String name, FailureAction action) {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(action, "action");
            if (name.isBlank()) {
                throw new IllegalArgumentException("Service name must not be blank");
            }
            if (definitions.putIfAbsent(name, action) != null) {
                throw new IllegalArgumentException("Duplicate service: " + name);
            }
            return this;
        }

        /**
         * Freezes the registry and creates a pending readiness controller.
         * @return the new controller
         */
        public ServiceReadiness build() {
            if (definitions.isEmpty()) {
                throw new IllegalStateException("At least one service is required");
            }
            if (definitions.containsValue(FailureAction.DISABLE_PLUGIN)
                    && (definitions.containsValue(FailureAction.BLOCK_CONNECTIONS)
                    || definitions.containsValue(FailureAction.STOP_SERVER))) {
                throw new IllegalStateException(
                        "Disabling a plugin would remove its connection or shutdown protection"
                );
            }
            return new ServiceReadiness(Collections.unmodifiableMap(new LinkedHashMap<>(definitions)));
        }
    }
}
