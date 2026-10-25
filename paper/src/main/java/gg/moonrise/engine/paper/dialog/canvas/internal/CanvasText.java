package gg.moonrise.engine.paper.dialog.canvas.internal;

import gg.moonrise.engine.message.Message;
import gg.moonrise.engine.message.util.MiniMessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.entity.Player;

/**
 * Resolves the loosely typed text values accepted by the canvas API.
 * <p>
 * Internal; not part of the supported API. Components are used as-is, {@link Message} values and
 * MiniMessage strings are resolved for the viewer, and anything else is shown via {@link String#valueOf(Object)}.
 */
public final class CanvasText {

    private CanvasText() {
    }

    /**
     * Resolves a value into a component.
     * @param viewer the player the text is rendered for, or {@code null}
     * @param value a {@link Component}, {@link ComponentLike}, {@link Message}, MiniMessage string, or other value
     * @return the component; empty for {@code null}
     */
    public static Component resolve(Player viewer, Object value) {
        if (value == null) return Component.empty();
        if (value instanceof Component component) return component;
        if (value instanceof Message message) return viewer == null ? message.asComponent() : message.asComponent(viewer);
        if (value instanceof ComponentLike like) return like.asComponent();
        if (value instanceof String text) return viewer == null ? MiniMessageUtil.fromText(text) : MiniMessageUtil.fromText(viewer, text);
        if (value instanceof Enum<?> constant) return Component.text(constant.name());

        return Component.text(String.valueOf(value));
    }

    /**
     * Resolves a value that may be absent.
     * @param viewer the player the text is rendered for
     * @param value the value, or {@code null}
     * @return the component, or {@code null}
     */
    public static Component resolveNullable(Player viewer, Object value) {
        return value == null ? null : resolve(viewer, value);
    }
}
