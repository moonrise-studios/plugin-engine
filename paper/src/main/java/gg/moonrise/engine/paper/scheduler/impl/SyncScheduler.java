package gg.moonrise.engine.paper.scheduler.impl;

import gg.moonrise.engine.paper.PaperPlugin;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Represents the SyncScheduler class.
 */

@RequiredArgsConstructor
public class SyncScheduler {

    private final PaperPlugin plugin;
    private final GlobalRegionScheduler scheduler;

    /**
     * Run a task synchronously on the main server thread.
     * @param task The task to run
     * @return The ScheduledTask representing the scheduled task
     */
    public ScheduledTask run(Consumer<ScheduledTask> task) {
        return scheduler.run(
                plugin,
                task
        );
    }

    /**
     * Run a task synchronously on the main server thread after a delay.
     * @param task The task to run
     * @param delayTicks The delay in ticks before running the task
     * @return The ScheduledTask representing the scheduled task
     */
    public ScheduledTask runDelayed(Consumer<ScheduledTask> task, long delayTicks) {
        return scheduler.runDelayed(
                plugin,
                task,
                delayTicks
        );
    }

    /**
     * Run a task synchronously on the main server thread after a delay.
     * @param task The task to run
     * @param delay The delay duration before running the task
     * @return The ScheduledTask representing the scheduled task
     */
    public ScheduledTask runDelayed(Consumer<ScheduledTask> task, Duration delay) {
        return runDelayed(
                task,
                delay.toMillis() / 50
        );
    }

    /**
     * Schedule a repeating task to run synchronously on the main server thread.
     * @param task The task to run
     * @param delayTicks The delay in ticks before running the task
     * @param periodTicks The period in ticks between successive runs of the task
     * @return The ScheduledTask representing the scheduled task
     */
    public ScheduledTask schedule(Consumer<ScheduledTask> task, long delayTicks, long periodTicks) {
        return scheduler.runAtFixedRate(
                plugin,
                task,
                delayTicks,
                periodTicks
        );
    }

    /**
     * Schedule a repeating task to run synchronously on the main server thread.
     * @param task The task to run
     * @param delay The delay duration before running the task
     * @return The ScheduledTask representing the scheduled task
     */
    public ScheduledTask schedule(Consumer<ScheduledTask> task, Duration delay) {
        return schedule(
                task,
                delay.toMillis() / 50,
                delay.toMillis() / 50
        );
    }

    /**
     * Execute a Runnable synchronously on the main server thread.
     * @param runnable The Runnable to execute
     */
    public void execute(Runnable runnable) {
        scheduler.execute(plugin, runnable);
    }

}
