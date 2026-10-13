package gg.moonrise.engine.paper.dialog.canvas;

import java.util.List;
import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * A Bedrock simple form rendered from a canvas, independent of the library that sends it.
 * <p>
 * Text uses section-sign formatting codes, which Bedrock forms display. The handlers may be called on
 * any thread; the session moves the work to the player's thread itself.
 * @param title the form title
 * @param content the text above the buttons
 * @param buttons the buttons, in order
 * @param onClick receives the index of the clicked button
 * @param onClose runs when the player closes the form without clicking a button
 */
public record BedrockForm(String title, String content, List<Button> buttons, IntConsumer onClick, Runnable onClose) {

    public BedrockForm {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(content, "content");
        buttons = List.copyOf(buttons);
        Objects.requireNonNull(onClick, "onClick");
        Objects.requireNonNull(onClose, "onClose");
    }

    /**
     * One form button.
     * @param text the label, which may span several lines
     * @param image the image shown beside it, or {@code null}
     */
    public record Button(String text, Image image) {

        public Button {
            Objects.requireNonNull(text, "text");
        }
    }

    /**
     * An image shown on a form button.
     * @param url whether {@code data} is a web address rather than a path inside the Bedrock resource pack
     * @param data the address or path
     */
    public record Image(boolean url, String data) {

        public Image {
            Objects.requireNonNull(data, "data");
        }
    }
}
