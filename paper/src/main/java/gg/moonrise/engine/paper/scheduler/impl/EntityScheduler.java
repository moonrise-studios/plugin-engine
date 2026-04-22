package gg.moonrise.engine.paper.scheduler.impl;

import gg.moonrise.engine.paper.PaperPlugin;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Entity;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Represents the EntityScheduler class.
 */

@RequiredArgsConstructor
public class EntityScheduler {

    private final PaperPlugin plugin;
    private final Entity entity;

    /**
     * Execute a Runnable task associated with the entity.
     * @param task The task to execute
     * @param retired The task to run when the main task is retired (optional)
     * @param delay The delay in ticks before executing the task
     * @return true if the task was successfully scheduled, false otherwise
     */
    public boolean execute(Runnable task, Runnable retired, long delay) {
        return scheduler().execute(
                plugin,
                task,
                retired,
                delay
        );
    }

    /**
     * Execute a Runnable task associated with the entity.
     * @param task The task to execute
     * @param retired The task to run when the main task is retired (optional)
     * @param delay The delay before executing the task
     * @return true if the task was successfully scheduled, false otherwise
     */
    public boolean execute(Runnable task, Runnable retired, Duration delay) {
        return scheduler().execute(
                plugin,
                task,
                retired,
                delay.toMillis() / 50
        );
    }

    /**
     * Execute a Runnable task associated with the entity.
     * @param task The task to execute
     * @param delay The delay in ticks before executing the task
     * @return true if the task was successfully scheduled, false otherwise
     */
    public boolean execute(Runnable task, long delay) {
        return execute(task, null, delay);
    }

    /**
     * Execute a Runnable task associated with the entity.
     * @param task The task to execute
     * @param delay The delay before executing the task
     * @return true if the task was successfully scheduled, false otherwise
     */
    public boolean execute(Runnable task, Duration delay) {
        return execute(task, null, delay);
    }

    /**
     * Run a task associated with the entity.
     * @param task The task to run
     * @param retired The task to run when the main task is retired (optional)
     * @return The ScheduledTask representing the scheduled task
     */
    public ScheduledTask run(Consumer<ScheduledTask> task, Runnable retired) {
        return scheduler().run(
                plugin,
                task,
                retired
        );
    }

    /**
     * Run a task associated with the entity.
     * @param task The task to run
     * @return The ScheduledTask representing the scheduled task
     */
    public ScheduledTask run(Consumer<ScheduledTask> task) {
        return run(task, null);
    }

    /**
     * Run a task associated with the entity after a delay.
     * @param task The task to run
     * @param retired The task to run when the main task is retired (optional)
     * @param delayTicks The delay in ticks before running the task
     * @return The ScheduledTask representing the scheduled task
     */
    public ScheduledTask runDelayed(Consumer<ScheduledTask> task, Runnable retired, long delayTicks) {
        return scheduler().runDelayed(
                plugin,
                task,
                retired,
                delayTicks
        );
    }

    /**
     * Run a task associated with the entity after a delay.
     * @param task The task to run
     * @param delayTicks The delay in ticks before running the task
     * @return The ScheduledTask representing the scheduled task
     */
    public ScheduledTask runDelayed(Consumer<ScheduledTask> task, long delayTicks) {
        return runDelayed(task, null, delayTicks);
    }

    /**
     * Run a task associated with the entity after a delay.
     * @param task The task to run
     * @param retired The task to run when the main task is retired (optional)
     * @param delay The delay before running the task
     * @return The ScheduledTask representing the scheduled task
     */
    public ScheduledTask runDelayed(Consumer<ScheduledTask> task, Runnable retired, Duration delay) {
        return scheduler().runDelayed(
                plugin,
                task,
                retired,
                delay.toMillis() / 50
        );
    }

    /**
     * Run a task associated with the entity after a delay.
     * @param task The task to run
     * @param delay The delay before running the task
     * @return The ScheduledTask representing the scheduled task
     */
    public ScheduledTask runDelayed(Consumer<ScheduledTask> task, Duration delay) {
        return runDelayed(task, null, delay);
    }

    /**
     * Schedule a repeating task associated with the entity.
     * @param task The task to run
     * @param retired The task to run when the main task is retired (optional)
     * @param delayTicks The delay in ticks before running the task
     * @param periodTicks The period in ticks between successive runs of the task
     * @return The ScheduledTask representing the scheduled task
     */
    public ScheduledTask schedule(Consumer<ScheduledTask> task, Runnable retired, long delayTicks, long periodTicks) {
        return scheduler().runAtFixedRate(
                plugin,
                task,
                retired,
                delayTicks,
                periodTicks
        );
    }

    /**
     * Schedule a repeating task associated with the entity.
     * @param task The task to run
     * @param delayTicks The delay in ticks before running the task
     * @param periodTicks The period in ticks between successive runs of the task
     * @return The ScheduledTask representing the scheduled task
     */
    public ScheduledTask schedule(Consumer<ScheduledTask> task, long delayTicks, long periodTicks) {
        return schedule(task, null, delayTicks, periodTicks);
    }

    /**
     * Schedule a repeating task associated with the entity.
     * @param task The task to run
     * @param retired The task to run when the main task is retired (optional)
     * @param delay The delay before running the task
     * @param period The period between successive runs of the task
     * @return The ScheduledTask representing the scheduled task
     */
    public ScheduledTask schedule(Consumer<ScheduledTask> task, Runnable retired, Duration delay, Duration period) {
        return scheduler().runAtFixedRate(
                plugin,
                task,
                retired,
                delay.toMillis() / 50,
                period.toMillis() / 50
        );
    }

    /**
     * Schedule a repeating task associated with the entity.
     * @param task The task to run
     * @param delay The delay before running the task
     * @param period The period between successive runs of the task
     * @return The ScheduledTask representing the scheduled task
     */
    public ScheduledTask schedule(Consumer<ScheduledTask> task, Duration delay, Duration period) {
        return schedule(task, null, delay, period);
    }

    /**
     * Get the underlying EntityScheduler
     * @return The EntityScheduler instance
     */
    public io.papermc.paper.threadedregions.scheduler.EntityScheduler scheduler() {
        return entity.getScheduler();
    }
}
