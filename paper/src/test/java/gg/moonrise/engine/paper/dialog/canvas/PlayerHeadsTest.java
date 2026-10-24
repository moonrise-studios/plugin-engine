package gg.moonrise.engine.paper.dialog.canvas;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PlayerHeadsTest {

    @Test
    public void aFetchThatCannotBeScheduledFailsAndTheNextOneTriesAgain() {
        PlayerHeads heads = new PlayerHeads(new RejectingExecutor());
        UUID playerId = UUID.randomUUID();

        CompletableFuture<HeadPixels> first = heads.fetch(playerId);
        CompletionException error = assertThrows(CompletionException.class, first::join);
        assertInstanceOf(IllegalStateException.class, error.getCause());

        CompletableFuture<HeadPixels> second = heads.fetch(playerId);
        assertNotSame(first, second, "the failed fetch must not stay in flight");
        assertTrue(second.isCompletedExceptionally());
        assertTrue(heads.cached(playerId).isEmpty());
    }

    @Test
    public void shutdownIsSafeBeforeAnyDownloadAndIdempotent() {
        PlayerHeads heads = new PlayerHeads(new RejectingExecutor());

        assertDoesNotThrow(heads::shutdown);
        assertDoesNotThrow(heads::shutdown);
    }

    /**
     * Schedules nothing, like the engine scheduler before it starts or while the plugin disables.
     */
    private static final class RejectingExecutor implements DialogExecutor {

        @Override
        public void async(Runnable task) {
            throw new IllegalStateException("Scheduler is not running");
        }

        @Override
        public void player(Player player, Runnable task, Runnable retired) {
            retired.run();
        }
    }
}
