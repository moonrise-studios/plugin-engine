package gg.moonrise.engine.job;

import java.time.Duration;

/**
 * Represents a job that can be scheduled to run at a later time or repeatedly.
 */
public interface Job {

    /**
     * Gets the interval at which this job should be executed. This is used for scheduling the job to run repeatedly.
     * @return The duration between each execution of the job.
     */
    Duration interval();

    /**
     * Gets the initial delay before the job is first executed. This is used for scheduling the job to run after a certain delay.
     * @return The duration to wait before the first execution of the job. Defaults to Duration.ZERO (no delay).
     */
    default Duration delay() {
        return Duration.ZERO;
    }

}
