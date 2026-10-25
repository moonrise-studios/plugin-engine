package gg.moonrise.engine.paper.dialog.canvas.internal;

import gg.moonrise.engine.paper.dialog.canvas.DialogUnavailableReason;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Detects Java clients too old to show dialogs.
 * <p>
 * Internal; not part of the supported API. PacketEvents reports the client version and is optional;
 * without it every client is assumed capable, which matches a server that does not accept older clients
 * through a protocol translator. A plugin that shades the engine needs a soft dependency on PacketEvents,
 * with {@code join-classpath} for Paper plugins, to see its API; when it cannot, that is logged once and
 * every client is assumed capable. Bedrock players are detected by {@link gg.moonrise.engine.paper.dialog.canvas.BedrockForms}.
 */
public final class DialogClientSupport {

    /**
     * Protocol version of Minecraft 1.21.6, the first release with dialogs.
     */
    public static final int MINIMUM_PROTOCOL = 771;

    private static final Logger LOGGER = LoggerFactory.getLogger(DialogClientSupport.class);
    private static final String PACKET_EVENTS_PLUGIN = "packetevents";

    private volatile boolean unlinked;

    /**
     * Checks whether a Java client can show dialogs.
     * @param player the player
     * @return the reason it cannot, or empty when it can
     */
    public Optional<DialogUnavailableReason> check(Player player) {
        if (unlinked || !enabled(PACKET_EVENTS_PLUGIN)) return Optional.empty();

        int protocol;
        try {
            protocol = PacketEventsProbe.protocol(player);
        } catch (LinkageError e) {
            unlink(e);
            return Optional.empty();
        } catch (RuntimeException e) {
            LOGGER.debug("PacketEvents could not report the client version of {}", player.getName(), e);
            return Optional.empty();
        }

        if (protocol > 0 && protocol < MINIMUM_PROTOCOL) return Optional.of(DialogUnavailableReason.UNSUPPORTED_CLIENT);
        return Optional.empty();
    }

    private void unlink(LinkageError error) {
        if (unlinked) return;

        unlinked = true;
        LOGGER.warn("PacketEvents is enabled but its API is not visible to this plugin, so dialogs cannot tell old clients apart. "
                + "Add PacketEvents as a soft dependency (join-classpath: true for Paper plugins). ({})", error.toString());
    }

    private static boolean enabled(String plugin) {
        try {
            return Bukkit.getPluginManager().isPluginEnabled(plugin);
        } catch (RuntimeException e) {
            LOGGER.debug("Could not query plugin {}", plugin, e);
            return false;
        }
    }
}
