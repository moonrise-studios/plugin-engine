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
     */
    BOTH
}
