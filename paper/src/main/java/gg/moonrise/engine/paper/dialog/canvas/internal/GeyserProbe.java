package gg.moonrise.engine.paper.dialog.canvas.internal;

import gg.moonrise.engine.paper.dialog.canvas.BedrockForm;
import org.bukkit.entity.Player;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.connection.GeyserConnection;

/**
 * Isolates the optional Geyser dependency, for Geyser running on this server without Floodgate.
 * <p>
 * Internal; not part of the supported API. Only referenced after checking that Geyser is enabled.
 */
public final class GeyserProbe {

    private GeyserProbe() {
    }

    /**
     * Returns whether a player is connected through Geyser.
     * @param player the player
     * @return whether the player is on Bedrock Edition
     */
    public static boolean isBedrock(Player player) {
        return GeyserApi.api().isBedrockPlayer(player.getUniqueId());
    }

    /**
     * Sends a form.
     * @param player the player
     * @param form the form
     * @return whether Geyser accepted it
     */
    public static boolean send(Player player, BedrockForm form) {
        return GeyserApi.api().sendForm(player.getUniqueId(), CumulusForms.simple(form));
    }

    /**
     * Closes every form the player has open, calling their close handlers.
     * @param player the player
     */
    public static void close(Player player) {
        GeyserConnection connection = GeyserApi.api().connectionByUuid(player.getUniqueId());
        if (connection != null) connection.closeForm();
    }
}
