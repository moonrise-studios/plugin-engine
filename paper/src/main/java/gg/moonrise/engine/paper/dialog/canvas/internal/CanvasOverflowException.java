package gg.moonrise.engine.paper.dialog.canvas.internal;

/**
 * A themed screen needs more text lines than its theme allows.
 * <p>
 * Internal; not part of the supported API. The session shows such a screen as a vanilla dialog instead.
 */
public final class CanvasOverflowException extends IllegalStateException {

    public CanvasOverflowException(String message) {
        super(message);
    }
}
