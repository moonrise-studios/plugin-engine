package gg.moonrise.engine.paper.dialog.canvas;

/**
 * How a {@link DialogMenu} is shown to one viewer. Chosen when the menu opens; the menu's
 * {@link DialogMenu#render render} code is the same for all three.
 */
public enum DialogPresentation {
    /**
     * The themed canvas: artwork drawn with the theme's font glyphs. Java clients with the theme's pack.
     */
    THEMED,
    /**
     * A vanilla dialog with the canvas text as its body and every clickable element as a dialog
     * button. Java clients without the theme's pack, when {@link DialogMenu#nativeFallback} allows it.
     */
    NATIVE,
    /**
     * A Bedrock simple form with the canvas text as its content and every clickable element as a
     * form button, sent through Floodgate. Bedrock players, when {@link DialogMenu#bedrockForms} allows it.
     */
    BEDROCK
}
