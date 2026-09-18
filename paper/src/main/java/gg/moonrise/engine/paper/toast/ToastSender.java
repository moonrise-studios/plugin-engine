package gg.moonrise.engine.paper.toast;

import org.bukkit.entity.Player;

/**
 * Internal delivery strategy for a single toast route.
 * <p>
 * Implementations are the only classes allowed to touch third-party APIs, and they are
 * only ever loaded once {@link Toasts} has confirmed the backing plugin is enabled.
 */
interface ToastSender {

    /**
     * Attempts to deliver the toast.
     * @param player the recipient
     * @param toast the toast to deliver
     * @return {@code true} when this route handled the toast
     */
    boolean send(Player player, Toast toast);
}
