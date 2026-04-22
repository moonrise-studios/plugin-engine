package gg.moonrise.engine.paper.scheduler;

import gg.moonrise.engine.paper.PaperPlugin;
import gg.moonrise.engine.paper.scheduler.impl.AsyncScheduler;
import gg.moonrise.engine.paper.scheduler.impl.EntityScheduler;
import gg.moonrise.engine.paper.scheduler.impl.LocationScheduler;
import gg.moonrise.engine.paper.scheduler.impl.SyncScheduler;
import org.bukkit.entity.Entity;

/**
 * Represents the Scheduler class.
 */

public class Scheduler {

    private static PaperPlugin INSTANCE;

    private static SyncScheduler SYNC_SCHEDULER;
    private static AsyncScheduler ASYNC_SCHEDULER;
    private static LocationScheduler LOCATION_SCHEDULER;

    /**
     * Initialize the Scheduler with the given plugin instance.
     * @param plugin The plugin instance to use for scheduling tasks.
     */
    public static void init(PaperPlugin plugin) {
        INSTANCE = plugin;
        SYNC_SCHEDULER = new SyncScheduler(plugin, plugin.getServer().getGlobalRegionScheduler());
        ASYNC_SCHEDULER = new AsyncScheduler(plugin, plugin.getServer().getAsyncScheduler());
        LOCATION_SCHEDULER = new LocationScheduler(plugin, plugin.getServer().getRegionScheduler());
    }

    /**
     * Get the Scheduler for an entity
     * @param entity The entity to get the scheduler for
     * @return The EntityScheduler for the given entity
     * @param <T> The type of the entity
     */
    public static <T extends Entity> EntityScheduler entity(T entity) {
        if (INSTANCE == null) {
            throw new IllegalStateException("Scheduler has not been initialized. Call Scheduler.init(...) before using Scheduler.entity().");
        }
        return new EntityScheduler(INSTANCE, entity);
    }

    /**
     * Get the global synchronous scheduler
     * @return  The SyncScheduler instance
     */
    public static SyncScheduler sync() {
        if (SYNC_SCHEDULER == null) {
            throw new IllegalStateException("Scheduler has not been initialized. Call Scheduler.init(...) before using Scheduler.sync().");
        }
        return SYNC_SCHEDULER;
    }

    /**
     * Get the global asynchronous scheduler
     * @return The AsyncScheduler instance
     */
    public static AsyncScheduler async() {
        if (ASYNC_SCHEDULER == null) {
            throw new IllegalStateException("Scheduler has not been initialized. Call Scheduler.init(...) before using Scheduler.async().");
        }
        return ASYNC_SCHEDULER;
    }

    /**
     * Get the location-based scheduler
     * @return The LocationScheduler instance
     */
    public static LocationScheduler location() {
        if (LOCATION_SCHEDULER == null) {
            throw new IllegalStateException("Scheduler has not been initialized. Call Scheduler.init(...) before using Scheduler.location().");
        }
        return LOCATION_SCHEDULER;
    }
}
