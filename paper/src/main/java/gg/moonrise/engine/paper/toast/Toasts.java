package gg.moonrise.engine.paper.toast;

import gg.moonrise.engine.message.Message;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Static facade for sending toast notifications to any player, on either edition.
 * <p>
 * Bedrock players receive a native Bedrock toast, either through the Geyser API when Geyser
 * runs on this server, or through Floodgate's raw packet channel when Geyser runs on a proxy
 * or standalone. Everyone else receives a transient fake advancement.
 * <p>
 * The Floodgate route is fire and forget: Floodgate hands the packet to Geyser over a plugin
 * channel and nothing reports back, so {@link ToastResult#BEDROCK} there means the packet was
 * handed off, not that the client displayed it.
 * <p>
 * None of Geyser, Floodgate, or PacketEvents is required at runtime; when none is available
 * the call is a no-op that returns {@link ToastResult#UNSUPPORTED}.
 */
public final class Toasts {

    private static final String GEYSER_PLUGIN = "Geyser-Spigot";
    private static final String FLOODGATE_PLUGIN = "floodgate";
    private static final String PACKET_EVENTS_PLUGIN = "packetevents";

    private static final AtomicBoolean WARNED = new AtomicBoolean();

    private static volatile ToastSender geyserSender;
    private static volatile ToastSender floodgateSender;
    private static volatile ToastSender packetEventsSender;

    private Toasts() {
    }

    /**
     * Creates a new toast builder.
     * @return a fresh builder
     */
    public static Toast.Builder builder() {
        return Toast.builder();
    }

    /**
     * Sends a toast to a player, picking the best available route.
     * @param player the recipient
     * @param toast the toast to send
     * @return how the toast was delivered
     */
    public static ToastResult send(Player player, Toast toast) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(toast, "toast");

        ToastSender geyser = geyser();
        if (geyser != null && geyser.send(player, toast)) return ToastResult.BEDROCK;

        ToastSender floodgate = floodgate();
        if (floodgate != null && floodgate.send(player, toast)) return ToastResult.BEDROCK;

        ToastSender packetEvents = packetEvents();
        if (packetEvents != null && packetEvents.send(player, toast)) return ToastResult.JAVA;

        warnOnce();
        return ToastResult.UNSUPPORTED;
    }

    /**
     * Sends a title-only toast built from a MiniMessage string.
     * @param player the recipient
     * @param title the MiniMessage title
     * @return how the toast was delivered
     */
    public static ToastResult send(Player player, String title) {
        return send(player, title, null);
    }

    /**
     * Sends a toast built from MiniMessage strings.
     * @param player the recipient
     * @param title the MiniMessage title
     * @param content the MiniMessage content, or {@code null}
     * @return how the toast was delivered
     */
    public static ToastResult send(Player player, String title, String content) {
        return send(player, Toast.builder().title(title).content(content).build());
    }

    /**
     * Sends a title-only toast built from a component.
     * @param player the recipient
     * @param title the title component
     * @return how the toast was delivered
     */
    public static ToastResult send(Player player, Component title) {
        return send(player, title, null);
    }

    /**
     * Sends a toast built from components.
     * @param player the recipient
     * @param title the title component
     * @param content the content component, or {@code null}
     * @return how the toast was delivered
     */
    public static ToastResult send(Player player, Component title, Component content) {
        return send(player, Toast.builder().title(title).content(content).build());
    }

    /**
     * Sends a title-only toast built from a message, resolved for the recipient.
     * @param player the recipient
     * @param title the title message
     * @return how the toast was delivered
     */
    public static ToastResult send(Player player, Message title) {
        return send(player, title, null);
    }

    /**
     * Sends a toast built from messages, resolved for the recipient.
     * @param player the recipient
     * @param title the title message
     * @param content the content message, or {@code null}
     * @return how the toast was delivered
     */
    public static ToastResult send(Player player, Message title, Message content) {
        return send(player, Toast.builder().title(title).content(content).build());
    }

    private static ToastSender geyser() {
        if (!pluginEnabled(GEYSER_PLUGIN)) return null;

        ToastSender sender = geyserSender;
        if (sender == null) {
            sender = new GeyserToastSender();
            geyserSender = sender;
        }
        return sender;
    }

    private static ToastSender floodgate() {
        if (!pluginEnabled(FLOODGATE_PLUGIN)) return null;

        ToastSender sender = floodgateSender;
        if (sender == null) {
            sender = new FloodgateToastSender();
            floodgateSender = sender;
        }
        return sender;
    }

    private static ToastSender packetEvents() {
        if (!pluginEnabled(PACKET_EVENTS_PLUGIN)) return null;

        ToastSender sender = packetEventsSender;
        if (sender == null) {
            sender = new PacketEventsToastSender();
            packetEventsSender = sender;
        }
        return sender;
    }

    private static boolean pluginEnabled(String name) {
        try {
            return Bukkit.getPluginManager().isPluginEnabled(name);
        } catch (IllegalStateException | NullPointerException exception) {
            return false;
        }
    }

    private static void warnOnce() {
        if (!WARNED.compareAndSet(false, true)) return;

        Bukkit.getLogger().warning("Toasts are unavailable (logged once): install PacketEvents for Java Edition toasts, "
                + "or Geyser (on this server) or Floodgate (when Geyser runs on a proxy or standalone) for native Bedrock toasts.");
    }
}
