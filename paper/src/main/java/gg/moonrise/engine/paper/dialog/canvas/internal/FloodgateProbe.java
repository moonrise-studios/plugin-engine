package gg.moonrise.engine.paper.dialog.canvas.internal;

import gg.moonrise.engine.paper.dialog.canvas.BedrockForm;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

/**
 * Isolates the optional Floodgate dependency.
 * <p>
 * Internal; not part of the supported API. Only referenced after checking that Floodgate is enabled.
 * Floodgate on this server reaches Geyser wherever it runs, on this server or on a proxy.
 */
public final class FloodgateProbe {

    private FloodgateProbe() {
    }

    /**
     * Returns whether a player joined through Floodgate.
     * @param player the player
     * @return whether the player is on Bedrock Edition
     */
    public static boolean isBedrock(Player player) {
        return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
    }

    /**
     * Sends a form.
     * @param player the player
     * @param form the form
     * @return whether Floodgate accepted it
     */
    public static boolean send(Player player, BedrockForm form) {
        return FloodgateApi.getInstance().sendForm(player.getUniqueId(), CumulusForms.simple(form));
    }

    /**
     * Closes every form the player has open, calling their close handlers.
     * @param player the player
     */
    public static void close(Player player) {
        FloodgateApi.getInstance().closeForm(player.getUniqueId());
    }
}
