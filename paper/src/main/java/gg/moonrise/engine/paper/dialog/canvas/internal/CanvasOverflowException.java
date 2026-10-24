package gg.moonrise.engine.paper.dialog.canvas.internal;

/**
 * A themed screen does not fit its theme: it needs more text lines than the theme allows, or a row of
 * tabs or cards is wider than the theme.
 * <p>
 * Internal; not part of the supported API. The session shows such a screen as a vanilla dialog instead.
 */
public final class CanvasOverflowException extends IllegalStateException {

    public CanvasOverflowException(String message) {
        super(message);
    }
}
