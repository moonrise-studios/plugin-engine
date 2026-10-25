package gg.moonrise.engine.paper.dialog.canvas.internal;

import gg.moonrise.engine.paper.dialog.canvas.DialogExecutor;
import gg.moonrise.engine.paper.scheduler.Scheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * {@link DialogExecutor} backed by the engine scheduler, safe on Folia.
 * <p>
 * Internal; not part of the supported API.
 */
public final class SchedulerDialogExecutor implements DialogExecutor {

    @Override
    public void async(Runnable task) {
        Scheduler.async().run(scheduled -> task.run());
    }

    @Override
    public void player(Player player, Runnable task, Runnable retired) {
        if (Bukkit.isOwnedByCurrentRegion(player)) {
            task.run();
            return;
        }

        ScheduledTask scheduled = Scheduler.entity(player).run(ignored -> task.run(), retired);
        if (scheduled == null) retired.run();
    }
}
