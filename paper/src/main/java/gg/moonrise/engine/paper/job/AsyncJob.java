package gg.moonrise.engine.paper.job;

import gg.moonrise.engine.job.Job;

/**
 * Represents a job that is executed asynchronously.
 * This interface extends the Job interface and is used to indicate that the job should be run on a separate thread, allowing for non-blocking operations.
 * Implementations of this interface should ensure that any interactions with the
 * Bukkit API are done in a thread-safe manner, as the Bukkit API is not thread-safe and should only be accessed from the main server thread.
 */
public interface AsyncJob extends Job, PaperTask {
}
