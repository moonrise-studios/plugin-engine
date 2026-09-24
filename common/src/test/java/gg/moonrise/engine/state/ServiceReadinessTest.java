package gg.moonrise.engine.state;

import gg.moonrise.engine.state.ServiceReadiness.Enforcement;
import gg.moonrise.engine.state.ServiceReadiness.Failure;
import gg.moonrise.engine.state.ServiceReadiness.FailureAction;
import gg.moonrise.engine.state.ServiceReadiness.Status;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ServiceReadinessTest {

    @Test
    public void blocksFromConstructionUntilTheRequiredServicePublishes() {
        ServiceReadiness readiness = ServiceReadiness.builder()
                .service("chat-runtime", FailureAction.BLOCK_CONNECTIONS)
                .build();

        assertTrue(readiness.snapshot().blocksConnections());
        assertEquals(Status.PENDING, readiness.snapshot().service("chat-runtime").status());

        readiness.ready("chat-runtime");
        assertFalse(readiness.snapshot().blocksConnections());
        assertTrue(readiness.snapshot().isReady("chat-runtime"));
    }

    @Test
    public void anInitialFailureBlocksButARejectedReloadKeepsTheLastGoodState() {
        ServiceReadiness readiness = ServiceReadiness.builder()
                .service("chat-runtime", FailureAction.BLOCK_CONNECTIONS)
                .build();
        Failure invalidVersion = new Failure("config schema version is newer", new IllegalStateException());
        readiness.unavailable("chat-runtime", invalidVersion);
        assertTrue(readiness.snapshot().blocksConnections());
        assertEquals(invalidVersion, readiness.snapshot().service("chat-runtime").failure().orElseThrow());
        assertThrows(IllegalStateException.class,
                () -> readiness.rejectCandidate("chat-runtime", invalidVersion));

        readiness.ready("chat-runtime");
        readiness.rejectCandidate("chat-runtime", invalidVersion);
        assertTrue(readiness.snapshot().isReady("chat-runtime"));
        assertFalse(readiness.snapshot().blocksConnections());
        assertEquals(invalidVersion, readiness.snapshot().service("chat-runtime").candidateFailure().orElseThrow());
        readiness.ready("chat-runtime");
        assertTrue(readiness.snapshot().service("chat-runtime").candidateFailure().isEmpty());
    }

    @Test
    public void losingActiveStateTriggersTheDeclaredAction() {
        ServiceReadiness readiness = ServiceReadiness.builder()
                .service("profiles", FailureAction.STOP_SERVER)
                .build();
        assertEquals(Enforcement.BLOCK_CONNECTIONS, readiness.snapshot().enforcement());
        readiness.ready("profiles");
        readiness.unavailable("profiles", new Failure("active state lost", new IllegalStateException()));
        assertEquals(Enforcement.STOP_SERVER, readiness.snapshot().enforcement());
    }

    @Test
    public void featureFailuresDoNotDisableTheWholePlugin() {
        ServiceReadiness readiness = ServiceReadiness.builder()
                .service("catalog", FailureAction.DISABLE_FEATURE)
                .build();
        readiness.unavailable("catalog", new Failure("invalid file", new IllegalStateException()));
        assertEquals(Enforcement.NONE, readiness.snapshot().enforcement());
        assertFalse(readiness.snapshot().isReady("catalog"));
    }

    @Test
    public void pluginDisablePolicyDoesNotRunBeforeFailure() {
        ServiceReadiness readiness = ServiceReadiness.builder()
                .service("config", FailureAction.DISABLE_PLUGIN)
                .build();
        assertEquals(Enforcement.NONE, readiness.snapshot().enforcement());
        readiness.unavailable("config", new Failure("invalid file", new IllegalStateException()));
        assertEquals(Enforcement.DISABLE_PLUGIN, readiness.snapshot().enforcement());
    }

    @Test
    public void deadlineExpiresOnlyPendingServicesAndIsIdempotent() {
        ServiceReadiness readiness = ServiceReadiness.builder()
                .service("chat-runtime", FailureAction.BLOCK_CONNECTIONS)
                .service("showcase", FailureAction.DISABLE_FEATURE)
                .build();
        readiness.ready("chat-runtime");
        Failure timeout = new Failure("startup deadline exceeded", new IllegalStateException());
        readiness.expirePending(timeout);
        long revision = readiness.snapshot().revision();
        assertTrue(readiness.snapshot().isReady("chat-runtime"));
        assertEquals(Status.UNAVAILABLE, readiness.snapshot().service("showcase").status());
        assertFalse(readiness.snapshot().blocksConnections());
        readiness.expirePending(timeout);
        assertEquals(revision, readiness.snapshot().revision());
    }

    @Test
    public void aPluginCannotDisableItsOwnCriticalGuard() {
        assertThrows(IllegalStateException.class, () -> ServiceReadiness.builder()
                .service("chat", FailureAction.BLOCK_CONNECTIONS)
                .service("optional-storage", FailureAction.DISABLE_PLUGIN)
                .build());
    }

    @Test
    public void observersReceiveSnapshotsAndCanBeClosed() {
        ServiceReadiness readiness = ServiceReadiness.builder()
                .service("runtime", FailureAction.BLOCK_CONNECTIONS)
                .build();
        List<Long> revisions = new ArrayList<>();
        ServiceReadiness.Subscription subscription = readiness.observe(snapshot -> revisions.add(snapshot.revision()));
        readiness.ready("runtime");
        subscription.close();
        readiness.unavailable("runtime", new Failure("lost", new IllegalStateException()));
        assertEquals(List.of(1L), revisions);
    }

    @Test
    public void concurrentPublicationDoesNotLoseAServiceTransition() {
        ServiceReadiness readiness = ServiceReadiness.builder()
                .service("config", FailureAction.BLOCK_CONNECTIONS)
                .service("storage", FailureAction.BLOCK_CONNECTIONS)
                .build();
        CompletableFuture<Void> config = CompletableFuture.runAsync(() -> readiness.ready("config"));
        CompletableFuture<Void> storage = CompletableFuture.runAsync(() -> readiness.ready("storage"));
        CompletableFuture.allOf(config, storage).join();
        assertTrue(readiness.snapshot().isReady("config"));
        assertTrue(readiness.snapshot().isReady("storage"));
        assertFalse(readiness.snapshot().blocksConnections());
        assertEquals(2L, readiness.snapshot().revision());
    }

    @Test
    public void stopIsTerminalAndClosesAdmission() {
        ServiceReadiness readiness = ServiceReadiness.builder()
                .service("runtime", FailureAction.BLOCK_CONNECTIONS)
                .build();
        readiness.ready("runtime");
        readiness.stop();
        assertTrue(readiness.snapshot().blocksConnections());
        assertThrows(IllegalStateException.class, () -> readiness.ready("runtime"));
    }
}
