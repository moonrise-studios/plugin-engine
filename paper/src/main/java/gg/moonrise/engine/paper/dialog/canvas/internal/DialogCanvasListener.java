package gg.moonrise.engine.paper.dialog.canvas.internal;

import gg.moonrise.engine.paper.dialog.canvas.DialogCanvasRuntime;
import gg.moonrise.engine.paper.dialog.canvas.DialogCloseReason;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

/**
 * Feeds pack status into the runtime and ends sessions of players who leave.
 * <p>
 * Internal; not part of the supported API.
 */
public final class DialogCanvasListener implements Listener {

    private final DialogCanvasRuntime runtime;

    public DialogCanvasListener(DialogCanvasRuntime runtime) {
        this.runtime = runtime;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPackStatus(PlayerResourcePackStatusEvent event) {
        runtime.packs().handle(event.getPlayer().getUniqueId(), event.getID(), event.getStatus());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        runtime.sessions().dispose(event.getPlayer().getUniqueId(), DialogCloseReason.QUIT);
        runtime.packs().forget(event.getPlayer().getUniqueId());
    }
}
