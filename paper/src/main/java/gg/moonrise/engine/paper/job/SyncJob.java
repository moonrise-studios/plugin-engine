package gg.moonrise.engine.paper.job;

import gg.moonrise.engine.job.Job;

/**
 * Represents a job that is executed synchronously.
 * This interface extends the Job interface and is used to indicate that the job should be run on the main server thread, allowing for operations that interact with the Bukkit API without needing to worry about thread safety.
 * Implementations of this interface should ensure that they do not perform long-running tasks, as this could cause server lag.
 */
public interface SyncJob extends Job, PaperTask {

}
