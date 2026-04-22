package gg.moonrise.engine.paper.job;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

/**
 * Represents a task that can be scheduled to run at a later time or repeatedly in a Paper plugin.
 * This interface defines a single method, tick, which will be called by the scheduler when the
 * task is executed. Implementations of this interface should provide the logic for what should happen
 * when the task is run, and can use the ScheduledTask parameter to manage the task's lifecycle (e.g., canceling it if needed).
 */
public interface PaperTask {

    /**
     * The method to be executed when the task is run.
     * This method will be called by the scheduler at the appropriate times based on the job's configuration.
     * @param task The ScheduledTask instance representing the currently running task, which can be used for managing the task's lifecycle (e.g., canceling it if needed).
     */
    void tick(ScheduledTask task);

}
