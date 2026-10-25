package gg.moonrise.engine.paper.dialog.canvas.internal;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

/**
 * Click and hover behaviour attached to every glyph of one canvas element.
 * <p>
 * Internal; not part of the supported API.
 * @param click the click event, or {@code null}
 * @param tooltip the hover text, or {@code null}
 */
public record Interaction(ClickEvent click, Component tooltip) {

    public static final Interaction NONE = new Interaction(null, null);

    /**
     * Applies this interaction to a component.
     * @param component the component
     * @return the styled component
     */
    public Component apply(Component component) {
        Component result = component;
        if (click != null) result = result.clickEvent(click);
        if (tooltip != null) result = result.hoverEvent(HoverEvent.showText(tooltip));

        return result;
    }

    /**
     * Uses this interaction's click and tooltip, falling back to another interaction's values.
     * @param fallback the fallback interaction
     * @return the combined interaction
     */
    public Interaction or(Interaction fallback) {
        return new Interaction(click != null ? click : fallback.click, tooltip != null ? tooltip : fallback.tooltip);
    }
}
