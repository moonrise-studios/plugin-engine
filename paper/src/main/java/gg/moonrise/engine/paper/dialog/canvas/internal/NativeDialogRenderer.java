package gg.moonrise.engine.paper.dialog.canvas.internal;

import gg.moonrise.engine.paper.dialog.canvas.DialogCanvas;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Turns a canvas into a vanilla dialog, for Java players without the theme's pack.
 * <p>
 * Internal; not part of the supported API. The canvas text becomes plain-message bodies, consecutive
 * lines sharing one body, and every clickable element becomes a dialog button in reading order. Entries
 * that only show information, such as leaderboard cards, become text. Geyser translates this dialog
 * into a Bedrock form on its own, so it also serves Bedrock players when Floodgate cannot be used.
 */
public final class NativeDialogRenderer {

    private static final TextReplacementConfig LINE_BREAKS = TextReplacementConfig.builder()
            .match(Pattern.compile("\\R"))
            .replacement(" - ")
            .build();

    private NativeDialogRenderer() {
    }

    /**
     * Renders a canvas.
     * @param canvas the described screen
     * @param title the dialog title
     * @param layout the button and body sizes
     * @param actions creates the dialog action for a server-side click
     * @return the dialog
     */
    public static Dialog render(DialogCanvas canvas, Component title, Layout layout, Function<Runnable, DialogAction> actions) {
        TextColor selectedColor = canvas.theme().theme().selectedTextColor();
        List<DialogBody> bodies = new ArrayList<>();
        List<Component> pending = new ArrayList<>();
        List<ActionButton> buttons = new ArrayList<>();

        for (CanvasOutline.Entry entry : canvas.outline().entries()) {
            if (entry instanceof CanvasOutline.Line line) {
                pending.add(line.text());
                continue;
            }

            CanvasOutline.Button button = (CanvasOutline.Button) entry;
            Component label = button.label().replaceText(LINE_BREAKS);
            if (button.action() == null) {
                pending.add(label);
                continue;
            }

            // Text separated by buttons on the canvas stays in separate bodies
            flush(pending, bodies, layout.bodyWidth());
            if (button.selected()) label = selected(label, selectedColor);
            buttons.add(ActionButton.create(label, button.tooltip(), layout.buttonWidth(), action(button.action(), actions)));
        }
        flush(pending, bodies, layout.bodyWidth());

        DialogBase base = DialogBase.builder(title)
                .canCloseWithEscape(true)
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.NONE)
                .body(bodies)
                .build();

        DialogType type = type(buttons, canvas.exitButton(), layout);
        return Dialog.create(builder -> builder.empty()
                .base(base)
                .type(type));
    }

    private static DialogType type(List<ActionButton> buttons, DialogCanvas.ExitButton exit, Layout layout) {
        ActionButton exitButton = exit == null
                ? null
                : ActionButton.create(exit.label(), exit.tooltip(), exit.width(), exit.action());

        if (!buttons.isEmpty()) {
            return DialogType.multiAction(buttons).columns(layout.columns()).exitAction(exitButton).build();
        }
        if (exitButton != null) return DialogType.notice(exitButton);

        return DialogType.dialogList(RegistrySet.keySet(RegistryKey.DIALOG, List.<TypedKey<Dialog>>of()), null, 1, layout.buttonWidth());
    }

    private static DialogAction action(CanvasAction action, Function<Runnable, DialogAction> actions) {
        return action.isLink()
                ? DialogAction.staticAction(ClickEvent.openUrl(action.url()))
                : actions.apply(action.run());
    }

    private static Component selected(Component label, TextColor color) {
        return Component.text()
                .append(Component.text("» ", color))
                .append(label.colorIfAbsent(color))
                .append(Component.text(" «", color))
                .build();
    }

    private static void flush(List<Component> lines, List<DialogBody> bodies, int width) {
        // Bodies are already spaced apart, so blank lines at their edges would only add more space
        while (!lines.isEmpty() && lines.get(0).equals(Component.empty())) lines.remove(0);
        while (!lines.isEmpty() && lines.get(lines.size() - 1).equals(Component.empty())) lines.remove(lines.size() - 1);
        if (lines.isEmpty()) return;

        bodies.add(DialogBody.plainMessage(Component.join(JoinConfiguration.newlines(), lines), width));
        lines.clear();
    }

    /**
     * Sizes of the vanilla dialog.
     * @param columns the button columns, from 1
     * @param buttonWidth the width of every button, from 1 to 1024
     * @param bodyWidth the width of the text, from 1 to 1024
     */
    public record Layout(int columns, int buttonWidth, int bodyWidth) {

        public Layout {
            if (columns < 1) throw new IllegalArgumentException("Native dialog columns must be positive");
            if (buttonWidth < 1 || buttonWidth > 1024) throw new IllegalArgumentException("Native button width must be between 1 and 1024");
            if (bodyWidth < 1 || bodyWidth > 1024) throw new IllegalArgumentException("Native body width must be between 1 and 1024");
        }
    }
}
