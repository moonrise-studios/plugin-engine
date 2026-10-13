package gg.moonrise.engine.paper.dialog.canvas.support;

import gg.moonrise.engine.paper.dialog.canvas.DialogExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs player work inline and queues background work until a test releases it.
 */
public final class TestDialogExecutor implements DialogExecutor {

    private final List<Runnable> pending = new ArrayList<>();

    @Override
    public void async(Runnable task) {
        pending.add(task);
    }

    @Override
    public void player(Player player, Runnable task, Runnable retired) {
        if (player.isOnline()) {
            task.run();
        } else {
            retired.run();
        }
    }

    public int pending() {
        return pending.size();
    }

    /**
     * Runs one queued background task.
     * @param index the task's position in the queue
     */
    public void run(int index) {
        pending.remove(index).run();
    }

    /**
     * Runs queued background tasks in order until none are left.
     */
    public void runAll() {
        while (!pending.isEmpty()) {
            run(0);
        }
    }
}
