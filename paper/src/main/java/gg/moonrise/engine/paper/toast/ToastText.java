package gg.moonrise.engine.paper.toast;

import gg.moonrise.engine.message.Message;
import gg.moonrise.engine.message.util.MiniMessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.function.Function;

/**
 * A deferred piece of toast text.
 * <p>
 * Internal; not part of the supported API. MiniMessage strings and {@link Message} values are
 * resolved per viewer at send time, while raw components are used as-is.
 */
public final class ToastText {

    private final Function<Player, Component> resolver;

    private ToastText(Function<Player, Component> resolver) {
        this.resolver = resolver;
    }

    /**
     * Wraps an already rendered component.
     * @param component the component
     * @return the wrapped text
     */
    public static ToastText of(Component component) {
        Objects.requireNonNull(component, "component");
        return new ToastText(viewer -> component);
    }

    /**
     * Wraps a message, resolved per viewer at send time.
     * @param message the message
     * @return the wrapped text
     */
    public static ToastText of(Message message) {
        Objects.requireNonNull(message, "message");
        return new ToastText(message::asComponent);
    }

    /**
     * Wraps a MiniMessage string, resolved per viewer at send time.
     * @param miniMessage the MiniMessage string
     * @return the wrapped text
     */
    public static ToastText of(String miniMessage) {
        Objects.requireNonNull(miniMessage, "miniMessage");
        return new ToastText(viewer -> MiniMessageUtil.fromText(viewer, miniMessage));
    }

    /**
     * Wraps a component that may be absent.
     * @param component the component, or {@code null}
     * @return the wrapped text, or {@code null}
     */
    public static ToastText nullable(Component component) {
        return component == null ? null : of(component);
    }

    /**
     * Wraps a message that may be absent.
     * @param message the message, or {@code null}
     * @return the wrapped text, or {@code null}
     */
    public static ToastText nullable(Message message) {
        return message == null ? null : of(message);
    }

    /**
     * Wraps a MiniMessage string that may be absent.
     * @param miniMessage the MiniMessage string, or {@code null}
     * @return the wrapped text, or {@code null}
     */
    public static ToastText nullable(String miniMessage) {
        return miniMessage == null ? null : of(miniMessage);
    }

    /**
     * Resolves this text for a viewer.
     * @param viewer the player the text is rendered for
     * @return the rendered component
     */
    public Component resolve(Player viewer) {
        return resolver.apply(viewer);
    }
}
