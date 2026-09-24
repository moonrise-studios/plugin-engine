package gg.moonrise.engine.paper.toast;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.unsafe.Unsafe;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

/**
 * Delivers native Bedrock toasts through Floodgate's raw packet channel.
 * <p>
 * Internal; not part of the supported API. This is the only class that imports Floodgate, and
 * {@link Toasts} never references it unless the {@code floodgate} plugin is enabled.
 * <p>
 * This route exists for setups where Geyser runs on a proxy or standalone, so the backend has
 * no Geyser API but does have Floodgate. The encoded {@code ToastRequestPacket} body is handed
 * to Floodgate, which prepends the packet id and forwards it over its plugin channel until
 * Geyser injects it into the Bedrock connection.
 * <p>
 * Delivery is fire and forget: nothing downstream reports back, so a {@code true} result only
 * means the packet was handed off, not that the client displayed it.
 * <p>
 * Floodgate's {@code FloodgateApi} and {@code Unsafe} are marked
 * {@code @Deprecated(forRemoval = true, since = "3.0.0")} on the Floodgate development branch.
 * Revisit this class when Floodgate 3.0 ships a supported replacement.
 */
public final class FloodgateToastSender implements ToastSender {

    private static final AtomicBoolean WARNED = new AtomicBoolean();

    /**
     * Cached because {@code FloodgateApi#unsafe()} logs a warning, allocates, and inspects the
     * call stack on every invocation.
     */
    private volatile Unsafe unsafe;

    /**
     * Creates a sender. Only construct this once the {@code floodgate} plugin is enabled.
     */
    public FloodgateToastSender() {
    }

    /**
     * Attempts to deliver the toast as a raw Bedrock packet.
     * @param player the recipient
     * @param toast the toast to deliver
     * @return {@code true} when the packet was handed to Floodgate
     */
    @Override
    public boolean send(Player player, Toast toast) {
        try {
            FloodgateApi api = FloodgateApi.getInstance();
            if (api == null) return false;

            // Floodgate does not validate the target and swallows an NPE for unknown players,
            // so the Bedrock check has to happen here.
            UUID uuid = player.getUniqueId();
            if (!api.isFloodgatePlayer(uuid)) return false;

            Unsafe target = unsafe(api);
            if (target == null) return false;

            target.sendPacket(
                    uuid,
                    BedrockToastPayload.TOAST_REQUEST_PACKET_ID,
                    BedrockToastPayload.encode(toast.bedrockTitle(player), toast.bedrockContent(player))
            );
            return true;
        } catch (LinkageError | RuntimeException exception) {
            warnOnce(exception);
            return false;
        }
    }

    private Unsafe unsafe(FloodgateApi api) {
        Unsafe cached = unsafe;
        if (cached != null) return cached;

        // Called directly rather than through a lambda or reflection: Floodgate captures the
        // calling class from the stack when handing out the unsafe API.
        Unsafe resolved = api.unsafe();
        unsafe = resolved;
        return resolved;
    }

    private static void warnOnce(Throwable throwable) {
        if (!WARNED.compareAndSet(false, true)) return;

        Bukkit.getLogger().log(Level.WARNING, "Bedrock toast delivery through Floodgate failed (logged once); falling back to the Java advancement path. "
                + "The usual cause is a plugin descriptor that declares 'floodgate' without joinClasspath = true.", throwable);
    }
}
