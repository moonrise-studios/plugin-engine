package gg.moonrise.engine.paper.dialog.canvas.internal;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import org.bukkit.entity.Player;

/**
 * Isolates the optional PacketEvents dependency.
 * <p>
 * Internal; not part of the supported API. Only referenced after checking that PacketEvents is enabled.
 */
public final class PacketEventsProbe {

    private PacketEventsProbe() {
    }

    /**
     * Returns a player's client protocol version.
     * @param player the player
     * @return the protocol version, or {@code -1} when unknown
     */
    public static int protocol(Player player) {
        ClientVersion version = PacketEvents.getAPI().getPlayerManager().getClientVersion(player);
        if (version == null) return -1;

        int protocol = version.getProtocolVersion();
        return protocol > 0 ? protocol : -1;
    }
}
