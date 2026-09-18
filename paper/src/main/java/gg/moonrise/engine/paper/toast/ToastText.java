package gg.moonrise.engine.paper.toast;

import gg.moonrise.engine.message.Message;
import gg.moonrise.engine.message.util.MiniMessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.function.Function;

/**
 * A deferred piece of toast text. MiniMessage strings and {@link Message} values are
 * resolved per viewer at send time, while raw components are used as-is.
 */
final class ToastText {

    private final Function<Player, Component> resolver;

    private ToastText(Function<Player, Component> resolver) {
        this.resolver = resolver;
    }

    static ToastText of(Component component) {
        Objects.requireNonNull(component, "component");
        return new ToastText(viewer -> component);
    }

    static ToastText of(Message message) {
        Objects.requireNonNull(message, "message");
        return new ToastText(message::asComponent);
    }

    static ToastText of(String miniMessage) {
        Objects.requireNonNull(miniMessage, "miniMessage");
        return new ToastText(viewer -> MiniMessageUtil.fromText(viewer, miniMessage));
    }

    static ToastText nullable(Component component) {
        return component == null ? null : of(component);
    }

    static ToastText nullable(Message message) {
        return message == null ? null : of(message);
    }

    static ToastText nullable(String miniMessage) {
        return miniMessage == null ? null : of(miniMessage);
    }

    Component resolve(Player viewer) {
        return resolver.apply(viewer);
    }
}
