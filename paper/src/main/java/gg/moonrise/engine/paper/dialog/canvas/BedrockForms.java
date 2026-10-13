package gg.moonrise.engine.paper.dialog.canvas;

import gg.moonrise.engine.paper.dialog.canvas.internal.GeyserForms;
import org.bukkit.entity.Player;

/**
 * Detects Bedrock players and sends them {@link BedrockForm}s.
 * <p>
 * The default, {@link #automatic()}, uses Floodgate when it is enabled on this server, which reaches
 * Geyser wherever it runs, and otherwise Geyser itself when it runs on this server; with neither,
 * nobody is a Bedrock player. A plugin that shades the engine needs soft dependencies on
 * {@code floodgate} and {@code Geyser-Spigot}, with {@code join-classpath} for Paper plugins, so its
 * class loader can see their APIs.
 * <p>
 * The Bedrock client draws the form with its own built-in screen: a scrolling list of 32 pixel rows,
 * each with an optional 32 x 32 image beside a button whose label shows at most two lines. Forms
 * stack on the client, so a form sent while another is open waits behind it; the session closes its
 * open form before sending a new one.
 */
public interface BedrockForms {

    /**
     * Returns whether a player joined from Bedrock Edition.
     * @param player the player
     * @return whether the player is on Bedrock Edition
     */
    public boolean isBedrock(Player player);

    /**
     * Shows a form. A form the player already has open stays in front until it is answered or closed.
     * @param player the player
     * @param form the form
     * @return whether the form was sent
     */
    public boolean send(Player player, BedrockForm form);

    /**
     * Closes every form the player has open, calling their close handlers with no response.
     * @param player the player
     */
    public void close(Player player);

    /**
     * Forms through Floodgate, or through Geyser on this server, detected at each call.
     * @return the default implementation
     */
    public static BedrockForms automatic() {
        return new GeyserForms();
    }
}
