package gg.moonrise.engine.paper.dialog.canvas.internal;

import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A canvas reduced to text lines and buttons, in reading order, for screens that cannot draw the
 * theme's artwork: the vanilla dialog shown to Java players without the pack, and Bedrock forms.
 * <p>
 * Internal; not part of the supported API. Artwork is left out and every clickable element becomes a
 * button, so a menu written once still offers all of its actions on every client.
 */
public final class CanvasOutline {

    private final List<Entry> entries = new ArrayList<>();

    /**
     * Adds a line of text.
     * @param text the text
     */
    public void line(Component text) {
        entries.add(new Line(Objects.requireNonNull(text, "text")));
    }

    /**
     * Adds an empty line, unless the outline is empty or already ends with one.
     */
    public void blank() {
        if (entries.isEmpty()) return;
        if (entries.get(entries.size() - 1) instanceof Line line && line.text().equals(Component.empty())) return;

        entries.add(new Line(Component.empty()));
    }

    /**
     * Adds a button.
     * @param button the button
     */
    public void button(Button button) {
        entries.add(Objects.requireNonNull(button, "button"));
    }

    /**
     * Entries in reading order, without a trailing empty line.
     * @return the entries
     */
    public List<Entry> entries() {
        List<Entry> result = new ArrayList<>(entries);
        while (!result.isEmpty() && result.get(result.size() - 1) instanceof Line line && line.text().equals(Component.empty())) {
            result.remove(result.size() - 1);
        }

        return List.copyOf(result);
    }

    /**
     * One element of an outline.
     */
    public sealed interface Entry permits Line, Button {
    }

    /**
     * A line of text.
     * @param text the text, empty for a blank line
     */
    public record Line(Component text) implements Entry {
    }

    /**
     * A button, or a display-only entry with a face when {@code action} is {@code null}.
     * @param label the label
     * @param tooltip the hover text, or {@code null}
     * @param action what a click does, or {@code null} when the entry only shows information
     * @param selected whether the entry is the selected one of a group, such as the current tab
     * @param head the player whose face the entry shows, or {@code null}
     * @param image the theme asset the entry shows, such as an icon, or {@code null}
     */
    public record Button(Component label, Component tooltip, CanvasAction action, boolean selected, UUID head, String image) implements Entry {

        public Button {
            Objects.requireNonNull(label, "label");
        }
    }
}
