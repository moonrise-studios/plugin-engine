package gg.moonrise.engine.paper.toast;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Delivers toasts to Bedrock players through the native Geyser toast API.
 * <p>
 * Internal; not part of the supported API. This is the only class that imports Geyser, and
 * {@link Toasts} never references it unless the Geyser plugin is enabled on this server.
 */
public final class GeyserToastSender implements ToastSender {

    private static final boolean SEND_TOAST_SUPPORTED = resolveSendToastSupport();
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    /**
     * Creates a sender. Only construct this once the Geyser plugin is enabled.
     */
    public GeyserToastSender() {
    }

    /**
     * Attempts to deliver the toast natively to a Bedrock player on this server.
     * @param player the recipient
     * @param toast the toast to deliver
     * @return {@code true} when Geyser accepted the toast
     */
    @Override
    public boolean send(Player player, Toast toast) {
        if (!SEND_TOAST_SUPPORTED) return false;

        try {
            GeyserConnection connection = GeyserApi.api().connectionByUuid(player.getUniqueId());
            if (connection == null) return false;

            connection.sendToast(toast.bedrockTitle(player), toast.bedrockContent(player));
            return true;
        } catch (LinkageError | RuntimeException exception) {
            warnOnce(exception);
            return false;
        }
    }

    private static boolean resolveSendToastSupport() {
        try {
            GeyserConnection.class.getMethod("sendToast", String.class, String.class);
            return true;
        } catch (NoSuchMethodException | LinkageError | RuntimeException exception) {
            // Geyser builds older than the 2.11.3 API line have no native toast; those
            // players silently take the Java advancement path instead.
            return false;
        }
    }

    private static void warnOnce(Throwable throwable) {
        if (!WARNED.compareAndSet(false, true)) return;

        Bukkit.getLogger().log(Level.WARNING, "Bedrock toast delivery through Geyser failed (logged once); falling back to the Java advancement path. "
                + "The usual cause is a plugin descriptor that declares 'Geyser-Spigot' without joinClasspath = true.", throwable);
    }
}
