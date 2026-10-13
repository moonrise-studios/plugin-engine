package gg.moonrise.engine.paper.dialog.canvas.internal;

import gg.moonrise.engine.paper.dialog.canvas.BedrockForm;
import gg.moonrise.engine.paper.dialog.canvas.BedrockForms;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * {@link BedrockForms} through Floodgate, or through Geyser on this server when Floodgate is absent.
 * <p>
 * Internal; not part of the supported API. Floodgate goes first because it reaches Geyser on a proxy
 * too. With neither plugin nobody is a Bedrock player. A {@link LinkageError} means the calling plugin
 * cannot see that plugin's API, usually for lack of a soft dependency with {@code join-classpath}; it
 * is logged once and that plugin is skipped from then on.
 */
public final class GeyserForms implements BedrockForms {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeyserForms.class);

    // Lambdas, not method references: a method reference loads its class, and with it the plugin's API, at once
    private final Route floodgate = new Route("floodgate", player -> FloodgateProbe.isBedrock(player),
            (player, form) -> FloodgateProbe.send(player, form), player -> FloodgateProbe.close(player));
    private final Route geyser = new Route("Geyser-Spigot", player -> GeyserProbe.isBedrock(player),
            (player, form) -> GeyserProbe.send(player, form), player -> GeyserProbe.close(player));

    @Override
    public boolean isBedrock(Player player) {
        return floodgate.isBedrock(player) || geyser.isBedrock(player);
    }

    @Override
    public boolean send(Player player, BedrockForm form) {
        if (floodgate.isBedrock(player)) return floodgate.send(player, form);
        if (geyser.isBedrock(player)) return geyser.send(player, form);

        return false;
    }

    @Override
    public void close(Player player) {
        if (floodgate.isBedrock(player)) {
            floodgate.close(player);
        } else if (geyser.isBedrock(player)) {
            geyser.close(player);
        }
    }

    /**
     * One plugin's way to Bedrock players.
     */
    private static final class Route {

        private final String plugin;
        private final Function<Player, Boolean> detect;
        private final Sender sender;
        private final Consumer<Player> closer;
        private volatile boolean unlinked;

        private Route(String plugin, Function<Player, Boolean> detect, Sender sender, Consumer<Player> closer) {
            this.plugin = plugin;
            this.detect = detect;
            this.sender = sender;
            this.closer = closer;
        }

        private boolean isBedrock(Player player) {
            if (!available()) return false;

            try {
                return detect.apply(player);
            } catch (LinkageError e) {
                unlink(e);
                return false;
            }
        }

        private boolean send(Player player, BedrockForm form) {
            try {
                return sender.send(player, form);
            } catch (LinkageError e) {
                unlink(e);
                return false;
            }
        }

        private void close(Player player) {
            try {
                closer.accept(player);
            } catch (LinkageError e) {
                unlink(e);
            }
        }

        private boolean available() {
            if (unlinked) return false;

            try {
                return Bukkit.getPluginManager().isPluginEnabled(plugin);
            } catch (RuntimeException e) {
                LOGGER.debug("Could not query plugin {}", plugin, e);
                return false;
            }
        }

        private void unlink(LinkageError error) {
            if (unlinked) return;

            unlinked = true;
            LOGGER.warn("{} is enabled but its API is not visible to this plugin, so it cannot send dialog forms to Bedrock players. "
                    + "Add {} as a soft dependency (join-classpath: true for Paper plugins). ({})", plugin, plugin, error.toString());
        }
    }

    /**
     * Sends a form through one plugin.
     */
    @FunctionalInterface
    private interface Sender {

        public boolean send(Player player, BedrockForm form);
    }
}
