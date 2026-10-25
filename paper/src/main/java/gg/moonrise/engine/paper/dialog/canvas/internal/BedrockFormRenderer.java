package gg.moonrise.engine.paper.dialog.canvas.internal;

import gg.moonrise.engine.paper.dialog.canvas.BedrockForm;
import gg.moonrise.engine.paper.dialog.canvas.DialogCanvas;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Turns a canvas into a Bedrock simple form.
 * <p>
 * Internal; not part of the supported API. The canvas text becomes the form content and every
 * clickable element a button, in reading order, followed by the exit button. Cards with a face become
 * buttons with the face as their image even when they do nothing, since buttons are the only place a
 * form shows images; clicking one shows the form again.
 */
public final class BedrockFormRenderer {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private BedrockFormRenderer() {
    }

    /**
     * Renders a canvas.
     * @param canvas the described screen
     * @param title the form title
     * @param heads returns the face image address for a player, or {@code null}
     * @param images returns the Bedrock texture path for a theme asset, or {@code null}
     * @param handler receives the player's response
     * @return the form
     */
    public static BedrockForm render(DialogCanvas canvas, Component title, Function<UUID, String> heads,
                                     Function<String, String> images, Handler handler) {
        List<String> lines = new ArrayList<>();
        List<BedrockForm.Button> buttons = new ArrayList<>();
        List<CanvasOutline.Button> sources = new ArrayList<>();

        for (CanvasOutline.Entry entry : canvas.outline().entries()) {
            switch (entry) {
                case CanvasOutline.Line line -> lines.add(LEGACY.serialize(line.text()));
                case CanvasOutline.Button button -> {
                    buttons.add(new BedrockForm.Button(label(button), image(button, heads, images)));
                    sources.add(button);
                }
            }
        }

        DialogCanvas.ExitButton exit = canvas.exitButton();
        if (exit != null) buttons.add(new BedrockForm.Button(LEGACY.serialize(exit.label()), null));

        return new BedrockForm(LEGACY.serialize(title), String.join("\n", lines), buttons, index -> {
            if (index >= 0 && index < sources.size()) {
                handler.click(sources.get(index).action());
            } else if (exit != null && index == sources.size()) {
                handler.exit();
            } else {
                handler.close();
            }
        }, handler::close);
    }

    private static String label(CanvasOutline.Button button) {
        String text = LEGACY.serialize(button.label());
        return button.selected() ? "» " + text + " «" : text;
    }

    private static BedrockForm.Image image(CanvasOutline.Button button, Function<UUID, String> heads, Function<String, String> images) {
        if (button.head() != null) {
            String url = heads.apply(button.head());
            if (url != null) return new BedrockForm.Image(true, url);
        }
        if (button.image() != null) {
            String path = images.apply(button.image());
            if (path != null) return new BedrockForm.Image(false, path);
        }

        return null;
    }

    /**
     * Receives a form response. Called on whatever thread delivers the response.
     */
    public interface Handler {

        /**
         * A button other than the exit button was clicked.
         * @param action its action, or {@code null} for an entry that only shows information
         */
        public void click(CanvasAction action);

        /**
         * The exit button was clicked.
         */
        public void exit();

        /**
         * The form was closed without a click.
         */
        public void close();
    }
}
