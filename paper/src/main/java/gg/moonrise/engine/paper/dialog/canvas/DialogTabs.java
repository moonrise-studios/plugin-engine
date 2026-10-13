package gg.moonrise.engine.paper.dialog.canvas;

import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasAction;
import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasBlock;
import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasLayout;
import gg.moonrise.engine.paper.dialog.canvas.internal.CanvasOutline;
import gg.moonrise.engine.paper.dialog.canvas.internal.Interaction;
import gg.moonrise.engine.paper.dialog.canvas.theme.CompiledAsset;
import gg.moonrise.engine.paper.dialog.canvas.theme.DialogTheme;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A centred row of tabs with one selected value.
 * <p>
 * Labels and tooltips may be {@link Component}s, MiniMessage strings, {@link gg.moonrise.engine.message.Message}s,
 * or any other value, which is shown with {@link String#valueOf(Object)}. Tabs share the available width
 * and their labels are shortened when they do not fit. Without the theme's artwork, each tab is a
 * button and the selected one is marked.
 * @param <T> the tab value type
 */
public final class DialogTabs<T> implements CanvasBlock {

    private static final int LABEL_PADDING = 6;

    private final DialogCanvas canvas;
    private final List<T> values;
    private final T selected;
    private Function<? super T, ?> label = value -> value;
    private Function<? super T, ?> tooltip;
    private Consumer<? super T> onSelect;
    private int gap = 3;

    public DialogTabs(DialogCanvas canvas, List<T> values, T selected) {
        this.canvas = Objects.requireNonNull(canvas, "canvas");
        this.values = List.copyOf(values);
        this.selected = selected;
    }

    /**
     * Sets how each tab is labelled.
     * @param label returns the label for a value
     * @return this tab row
     */
    public DialogTabs<T> label(Function<? super T, ?> label) {
        this.label = Objects.requireNonNull(label, "label");
        return this;
    }

    /**
     * Sets the hover text of each tab.
     * @param tooltip returns the tooltip for a value, or {@code null} for none
     * @return this tab row
     */
    public DialogTabs<T> tooltip(Function<? super T, ?> tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    /**
     * Sets what happens when a tab is clicked, including the selected tab.
     * @param onSelect receives the clicked value
     * @return this tab row
     */
    public DialogTabs<T> onSelect(Consumer<? super T> onSelect) {
        this.onSelect = onSelect;
        return this;
    }

    /**
     * Sets the space between tabs.
     * @param gap the gap in pixels
     * @return this tab row
     */
    public DialogTabs<T> gap(int gap) {
        if (gap < 0) throw new IllegalArgumentException("Tab gap must not be negative");

        this.gap = gap;
        return this;
    }

    @Override
    public int render(CanvasLayout layout, int line) {
        if (values.isEmpty()) return 0;

        CompiledAsset normal = layout.theme().asset(DialogTheme.TAB);
        CompiledAsset active = layout.theme().asset(DialogTheme.TAB_SELECTED);
        DialogTheme theme = layout.theme().theme();

        List<Component> labels = new ArrayList<>(values.size());
        List<Integer> widths = new ArrayList<>(values.size());
        int total = gap * (values.size() - 1);
        for (T value : values) {
            boolean isSelected = Objects.equals(value, selected);
            Component text = canvas.component(label.apply(value))
                    .colorIfAbsent(isSelected ? theme.selectedTextColor() : theme.textColor());
            CompiledAsset asset = isSelected ? active : normal;
            int width = Math.max(layout.measurer().width(text) + LABEL_PADDING * 2, asset.minimumWidth());
            labels.add(text);
            widths.add(width);
            total += width;
        }

        int available = layout.contentWidth();
        if (total > available) {
            int minimum = Math.max(normal.minimumWidth(), active.minimumWidth());
            int each = (available - gap * (values.size() - 1)) / values.size();
            if (each < minimum) {
                throw new IllegalStateException(values.size() + " dialog tabs do not fit the theme width " + available);
            }

            total = gap * (values.size() - 1);
            for (int i = 0; i < widths.size(); i++) {
                widths.set(i, Math.min(widths.get(i), each));
                total += widths.get(i);
            }
        }

        int x = layout.contentLeft() + Math.max(0, (available - total) / 2);
        for (int i = 0; i < values.size(); i++) {
            T value = values.get(i);
            Interaction interaction = new Interaction(
                    onSelect == null ? null : canvas.action(() -> onSelect.accept(value)),
                    tooltip == null ? null : canvas.nullableComponent(tooltip.apply(value))
            );
            CompiledAsset asset = Objects.equals(value, selected) ? active : normal;
            layout.placeButton(line + 1, x, widths.get(i), asset, labels.get(i), interaction);
            x += widths.get(i) + gap;
        }

        return 3;
    }

    @Override
    public void outline(CanvasOutline outline) {
        for (T value : values) {
            Component text = canvas.component(label.apply(value));
            Component hover = tooltip == null ? null : canvas.nullableComponent(tooltip.apply(value));
            CanvasAction action = onSelect == null ? null : CanvasAction.run(() -> onSelect.accept(value));
            outline.button(new CanvasOutline.Button(text, hover, action, Objects.equals(value, selected), null, null));
        }
    }
}
