package gg.moonrise.engine.paper.toast;

/**
 * Selects which part of a {@link Toast} is rendered on the single customizable line
 * that Java Edition toasts expose.
 * <p>
 * If the selected part is absent, the other part is used instead.
 */
public enum ToastJavaLine {

    /**
     * Render the toast title.
     */
    TITLE,

    /**
     * Render the toast content.
     */
    CONTENT,

    /**
     * Render the title and the content joined by a single space.
     * <p>
     * Whether this stays on one line depends purely on how wide the combined text is.
     */
    BOTH,

    /**
     * Render the title and the content as two explicit lines, separated by a line break.
     * <p>
     * The vanilla client splits the advancement title at 125 px per line. When that split
     * yields two or more lines, the toast shows the fixed frame header ("Advancement Made!",
     * "Goal Reached!", "Challenge Complete!") for roughly the first 1.5 seconds, fades it out,
     * and then draws the custom lines in its place for the rest of the display time. A forced
     * line break therefore gives two fully custom lines in the toast's steady state without a
     * resource pack.
     * <p>
     * Each line is still limited to 125 px and may wrap further, so keep both lines short. The
     * frame continues to control the sound and the colour of the brief header, and the header
     * text itself cannot be changed from the server.
     * <p>
     * When the toast has no content this degrades to the title alone, which is a single line
     * and therefore keeps the normal permanent-header behaviour.
     */
    TWO_LINES
}
