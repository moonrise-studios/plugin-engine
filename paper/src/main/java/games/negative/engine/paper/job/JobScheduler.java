package games.negative.engine.paper.job;

import games.negative.engine.paper.PaperPlugin;
import games.negative.engine.paper.scheduler.Scheduler;
import games.negative.moss.spring.Enableable;
import games.negative.moss.spring.SpringComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * Represents the JobScheduler class.
 */

@Slf4j
@SpringComponent
@RequiredArgsConstructor
public class JobScheduler implements Enableable {

    private final PaperPlugin plugin;

    /**
     * Executes onEnable.
     */

    @Override
    public void onEnable() {
        log.info("Scheduling jobs...");
        plugin.invokeBeans(
                SyncJob.class,
                syncJob -> {
                    Scheduler.sync().schedule(syncJob::tick, toTicks(syncJob.delay()), toTicks(syncJob.interval()));
                    log.info("Scheduling job {} with delay {} and interval {}", syncJob.getClass().getSimpleName(), syncJob.delay(), syncJob.interval());
                }
        );

        plugin.invokeBeans(
                AsyncJob.class,
                asyncJob -> {
                    Scheduler.async().schedule(asyncJob::tick, asyncJob.delay(), asyncJob.interval());
                    log.info("Scheduling async job {} with delay {} and interval {}", asyncJob.getClass().getSimpleName(), asyncJob.delay(), asyncJob.interval());
                }
        );
    }

    /**
     * Converts a Duration to ticks (1 tick = 50ms)
     * @param duration The duration to convert
     * @return The duration in ticks
     */
    public long toTicks(Duration duration) {
        return duration.toMillis() / 50;
    }
}
