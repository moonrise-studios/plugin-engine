package gg.moonrise.engine.paper.dialog.canvas;

import gg.moonrise.engine.paper.dialog.canvas.internal.SchedulerDialogExecutor;
import org.bukkit.entity.Player;

/**
 * Threading used by dialog sessions: background work, and work that must run on a player's thread.
 */
public interface DialogExecutor {

    /**
     * Runs work off the server thread, for example database queries or skin downloads.
     * @param task the task
     */
    public void async(Runnable task);

    /**
     * Runs work on the thread that owns a player, immediately when already on it.
     * @param player the player
     * @param task the task
     * @param retired run instead of {@code task} when the player is no longer valid
     */
    public void player(Player player, Runnable task, Runnable retired);

    /**
     * The default executor, backed by the engine {@link gg.moonrise.engine.paper.scheduler.Scheduler}.
     * @return the executor
     */
    public static DialogExecutor scheduler() {
        return new SchedulerDialogExecutor();
    }
}
