package gg.moonrise.engine.paper.dialog.canvas;

/**
 * Why a dialog session ended.
 */
public enum DialogCloseReason {
    /**
     * Closed by plugin code through {@link DialogSession#close()}.
     */
    CLOSED,
    /**
     * The player used the exit button or pressed Escape on a screen with an exit button.
     */
    EXITED,
    /**
     * Another canvas dialog was opened for the same player.
     */
    REPLACED,
    /**
     * The player left the server.
     */
    QUIT,
    /**
     * The session outlived its timeout without being rendered again.
     */
    EXPIRED,
    /**
     * Rendering threw an exception.
     */
    FAILED,
    /**
     * The owning plugin disabled.
     */
    DISABLED
}
