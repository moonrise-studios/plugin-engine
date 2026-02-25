package games.negative.engine.paper.scheduler.impl;

import games.negative.engine.paper.PaperPlugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Represents the AsyncScheduler class.
 */

@RequiredArgsConstructor
public class AsyncScheduler {

    private final PaperPlugin plugin;
    private final io.papermc.paper.threadedregions.scheduler.AsyncScheduler scheduler;

    /**
     * Schedule a repeating task to run asynchronously.
     * @param task The task to run
     * @param period The period between successive runs of the task
     * @return The ScheduledTask representing the scheduled task
     */
    public ScheduledTask schedule(Consumer<ScheduledTask> task, Duration period) {
        return schedule(
                task,
                0,
                period.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Schedule a repeating task to run asynchronously.
     * @param task The task to run
     * @param delay The delay before running the task
     * @param period The period between successive runs of the task
     * @return The ScheduledTask representing the scheduled task
     */
    public ScheduledTask schedule(Consumer<ScheduledTask> task, Duration delay, Duration period) {
        return schedule(
                task,
                delay.toMillis(),
                period.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Schedule a repeating task to run asynchronously.
     * @param task The task to run
     * @param delayTicks The delay in ticks before running the task
     * @param periodTicks The period in ticks between successive runs of the task
     * @param unit The time unit for delay and period
     * @return The ScheduledTask representing the scheduled task
     */
    public ScheduledTask schedule(Consumer<ScheduledTask> task, long delayTicks, long periodTicks, TimeUnit unit) {
        return scheduler.runAtFixedRate(
                plugin,
                task,
                delayTicks,
                periodTicks,
                unit
        );
    }

    /**
     * Run a task asynchronously after a delay.
     * @param task The task to run
     * @param delay The delay before running the task
     * @return The ScheduledTask representing the scheduled task
     */
    public ScheduledTask runDelayed(Consumer<ScheduledTask> task, Duration delay) {
        return scheduler.runDelayed(
                plugin,
                task,
                delay.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * Run a task asynchronously after a delay.
     * @param task The task to run
     * @param delayTicks The delay in ticks before running the task
     * @param unit The time unit for the delay
     * @return The ScheduledTask representing the scheduled task
     */
    public ScheduledTask runDelayed(Consumer<ScheduledTask> task, long delayTicks, TimeUnit unit) {
        return scheduler.runDelayed(
                plugin,
                task,
                delayTicks,
                unit
        );
    }

    /**
     * Run a task asynchronously immediately.
     * @param task The task to run
     * @return The ScheduledTask representing the scheduled task
     */
    public ScheduledTask run(Consumer<ScheduledTask> task) {
        return scheduler.runNow(plugin, task);
    }

}
