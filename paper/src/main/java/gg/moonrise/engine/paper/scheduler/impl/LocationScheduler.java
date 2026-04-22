package gg.moonrise.engine.paper.scheduler.impl;

import gg.moonrise.engine.paper.PaperPlugin;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.RequiredArgsConstructor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Represents the LocationScheduler class.
 */

@RequiredArgsConstructor
public class LocationScheduler {

    private final PaperPlugin plugin;
    private final RegionScheduler scheduler;

    /**
     * Execute a task in the region of the given location
     * @param location The location to execute the task in
     * @param task The task to execute
     */
    public void executeLocation(Location location, Runnable task) {
        scheduler.execute(plugin, location, task);
    }

    /**
     * Execute a task in the region of the given chunk coordinates
     * @param world The world of the chunk
     * @param chunkX The x coordinate of the chunk
     * @param chunkZ The z coordinate of the chunk
     * @param task The task to execute
     */
    public void executeChunk(World world, int chunkX, int chunkZ, Runnable task) {
        scheduler.execute(
            plugin,
            world,
            chunkX,
            chunkZ,
            task
        );
    }

    /**
     * Execute a task in the region of the given chunk
     * @param chunk The chunk to execute the task in
     * @param task The task to execute
     */
    public void executeChunk(Chunk chunk, Runnable task) {
        executeChunk(
            chunk.getWorld(),
            chunk.getX(),
            chunk.getZ(),
            task
        );
    }

    /**
     * Run a scheduled task in the region of the given location
     * @param location The location to run the task in
     * @param task The task to run
     * @return The scheduled task
     */
    public ScheduledTask run(Location location, Consumer<ScheduledTask> task) {
        return scheduler.run(
            plugin,
            location,
            task
        );
    }

    /**
     * Run a scheduled task in the region of the given chunk coordinates
     * @param world The world of the chunk
     * @param chunkX The x coordinate of the chunk
     * @param chunkZ The z coordinate of the chunk
     * @param task The task to run
     * @return The scheduled task
     */
    public ScheduledTask run(World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task) {
        return scheduler.run(
            plugin,
            world,
            chunkX,
            chunkZ,
            task
        );
    }

    /**
     * Run a scheduled task in the region of the given chunk
     * @param chunk The chunk to run the task in
     * @param task The task to run
     * @return The scheduled task
     */
    public ScheduledTask run(Chunk chunk, Consumer<ScheduledTask> task) {
        return run(
                chunk.getWorld(),
                chunk.getX(),
                chunk.getZ(),
                task
        );
    }

    /**
     * Run a delayed scheduled task in the region of the given location
     * @param location The location to run the task in
     * @param delay The delay before running the task
     * @param task The task to run
     * @return The scheduled task
     */
    public ScheduledTask runDelayed(Location location, Duration delay, Consumer<ScheduledTask> task) {
        return runDelayed(
            location,
            delay.toMillis() / 50,
            task
        );
    }

    /**
     * Run a delayed scheduled task in the region of the given location
     * @param location The location to run the task in
     * @param delay The delay before running the task
     * @param task The task to run
     * @return The scheduled task
     */
    public ScheduledTask runDelayed(Location location, long delay, Consumer<ScheduledTask> task) {
        return scheduler.runDelayed(
            plugin,
            location,
            task,
            delay
        );
    }

    /**
     * Run a delayed scheduled task in the region of the given chunk coordinates
     * @param world The world of the chunk
     * @param chunkX The x coordinate of the chunk
     * @param chunkZ The z coordinate of the chunk
     * @param delay The delay before running the task
     * @param task The task to run
     * @return The scheduled task
     */
    public ScheduledTask runDelayed(World world, int chunkX, int chunkZ, Duration delay, Consumer<ScheduledTask> task) {
        return runDelayed(
                world,
                chunkX,
                chunkZ,
                delay.toMillis() / 50,
                task
        );
    }

    /**
     * Run a delayed scheduled task in the region of the given chunk coordinates
     * @param world The world of the chunk
     * @param chunkX The x coordinate of the chunk
     * @param chunkZ The z coordinate of the chunk
     * @param delay The delay before running the task
     * @param task The task to run
     * @return The scheduled task
     */
    public ScheduledTask runDelayed(World world, int chunkX, int chunkZ, long delay, Consumer<ScheduledTask> task) {
        return scheduler.runDelayed(
                plugin,
                world,
                chunkX,
                chunkZ,
                task,
                delay
        );
    }

    /**
     * Run a delayed scheduled task in the region of the given chunk
     * @param chunk The chunk to run the task in
     * @param delay The delay before running the task
     * @param task The task to run
     * @return The scheduled task
     */
    public ScheduledTask runDelayed(Chunk chunk, Duration delay, Consumer<ScheduledTask> task) {
        return runDelayed(
                chunk.getWorld(),
                chunk.getX(),
                chunk.getZ(),
                delay.toMillis() / 50,
                task
        );
    }

    /**
     * Run a delayed scheduled task in the region of the given chunk
     * @param chunk The chunk to run the task in
     * @param delay The delay before running the task
     * @param task The task to run
     * @return The scheduled task
     */
    public ScheduledTask runDelayed(Chunk chunk, long delay, Consumer<ScheduledTask> task) {
        return runDelayed(
                chunk.getWorld(),
                chunk.getX(),
                chunk.getZ(),
                delay,
                task
        );
    }

    /**
     * Schedule a repeating task in the region of the given location
     * @param location The location to run the task in
     * @param delay The delay before running the task
     * @param period The period between task executions
     * @param task The task to run
     * @return The scheduled task
     */
    public ScheduledTask schedule(Location location, Duration delay, Duration period, Consumer<ScheduledTask> task) {
        return schedule(
            location,
            delay.toMillis() / 50,
            period.toMillis() / 50,
            task
        );
    }

    /**
     * Schedule a repeating task in the region of the given location
     * @param location The location to run the task in
     * @param delay The delay before running the task
     * @param period The period between task executions
     * @param task The task to run
     * @return The scheduled task
     */
    public ScheduledTask schedule(Location location, long delay, long period, Consumer<ScheduledTask> task) {
        return scheduler.runAtFixedRate(
            plugin,
            location,
            task,
            delay,
            period
        );
    }

    /**
     * Schedule a repeating task in the region of the given chunk coordinates
     * @param world The world of the chunk
     * @param chunkX The x coordinate of the chunk
     * @param chunkZ The z coordinate of the chunk
     * @param delay The delay before running the task
     * @param period The period between task executions
     * @param task The task to run
     * @return The scheduled task
     */
    public ScheduledTask schedule(World world, int chunkX, int chunkZ, Duration delay, Duration period, Consumer<ScheduledTask> task) {
        return schedule(
                world,
                chunkX,
                chunkZ,
                delay.toMillis() / 50,
                period.toMillis() / 50,
                task
        );
    }

    /**
     * Schedule a repeating task in the region of the given chunk coordinates
     * @param world The world of the chunk
     * @param chunkX The x coordinate of the chunk
     * @param chunkZ The z coordinate of the chunk
     * @param delay The delay before running the task
     * @param period The period between task executions
     * @param task The task to run
     * @return The scheduled task
     */
    public ScheduledTask schedule(World world, int chunkX, int chunkZ, long delay, long period, Consumer<ScheduledTask> task) {
        return scheduler.runAtFixedRate(
                plugin,
                world,
                chunkX,
                chunkZ,
                task,
                delay,
                period
        );
    }

    /**
     * Schedule a repeating task in the region of the given chunk
     * @param chunk The chunk to run the task in
     * @param delay The delay before running the task
     * @param period The period between task executions
     * @param task The task to run
     * @return The scheduled task
     */
    public ScheduledTask schedule(Chunk chunk, Duration delay, Duration period, Consumer<ScheduledTask> task) {
        return schedule(
                chunk.getWorld(),
                chunk.getX(),
                chunk.getZ(),
                delay.toMillis() / 50,
                period.toMillis() / 50,
                task
        );
    }

    /**
     * Schedule a repeating task in the region of the given chunk
     * @param chunk The chunk to run the task in
     * @param delay The delay before running the task
     * @param period The period between task executions
     * @param task The task to run
     * @return The scheduled task
     */
    public ScheduledTask schedule(Chunk chunk, long delay, long period, Consumer<ScheduledTask> task) {
        return schedule(
                chunk.getWorld(),
                chunk.getX(),
                chunk.getZ(),
                delay,
                period,
                task
        );
    }

}
