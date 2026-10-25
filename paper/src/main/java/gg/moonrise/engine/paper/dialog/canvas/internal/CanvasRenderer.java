package gg.moonrise.engine.paper.dialog.canvas.internal;

import gg.moonrise.engine.paper.dialog.canvas.DialogAlign;
import gg.moonrise.engine.paper.dialog.canvas.DialogCanvas;
import gg.moonrise.engine.paper.dialog.canvas.theme.CompiledTheme;
import gg.moonrise.engine.paper.dialog.canvas.theme.DialogTheme;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.set.RegistrySet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;

import java.util.List;

/**
 * Turns a described {@link DialogCanvas} into a Paper dialog.
 * <p>
 * Internal; not part of the supported API. The screen is a single plain-message body: line 0 carries
 * only the panel glyphs, content starts on line 1, and one empty line pads the bottom. The dialog never
 * pauses and keeps itself open after body clicks, so actions decide what to show next.
 */
public final class CanvasRenderer {

    /**
     * Extra wrap width given to the body so client-side padding never wraps a full-width line.
     */
    public static final int BODY_WRAP_SLACK = 16;

    private static final int LIST_BUTTON_WIDTH = 150;

    private CanvasRenderer() {
    }

    /**
     * Renders a canvas.
     * @param canvas the described screen
     * @param title the dialog title
     * @return the rendered lines, body, and dialog
     */
    public static RenderedCanvas render(DialogCanvas canvas, Component title) {
        CompiledTheme compiled = canvas.theme();
        DialogTheme theme = compiled.theme();
        CanvasLayout layout = new CanvasLayout(compiled);

        int line = 1;
        for (CanvasBlock block : canvas.blocks()) {
            line += block.render(layout, line);
        }

        if (canvas.footer() != null) {
            line++;
            line += CanvasBlocks.text(List.of(canvas.footer()), DialogAlign.CENTER).render(layout, line);
        }

        int totalLines = Math.max(line, layout.lineCount()) + 1;
        if (totalLines > theme.maxLines()) {
            throw new CanvasOverflowException("Dialog canvas needs " + totalLines + " lines but theme "
                    + theme.namespace() + ":" + theme.id() + " allows " + theme.maxLines());
        }

        layout.placePanel(totalLines);
        List<Component> lines = layout.compose(totalLines);
        Component body = Component.join(JoinConfiguration.newlines(), lines);

        DialogBase base = DialogBase.builder(title)
                .canCloseWithEscape(true)
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.NONE)
                .body(List.of(DialogBody.plainMessage(body, Math.min(1024, theme.width() + BODY_WRAP_SLACK))))
                .build();

        DialogType type = type(canvas.exitButton());
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(base)
                .type(type));

        return new RenderedCanvas(lines, body, dialog);
    }

    private static DialogType type(DialogCanvas.ExitButton exit) {
        if (exit == null) {
            return DialogType.dialogList(RegistrySet.keySet(RegistryKey.DIALOG, List.<TypedKey<Dialog>>of()), null, 1, LIST_BUTTON_WIDTH);
        }

        return DialogType.notice(ActionButton.create(exit.label(), exit.tooltip(), exit.width(), exit.action()));
    }

    /**
     * Output of a render.
     * @param lines the composed body lines, each exactly the theme width
     * @param body the full body component
     * @param dialog the dialog to show
     */
    public record RenderedCanvas(List<Component> lines, Component body, Dialog dialog) {
    }
}
