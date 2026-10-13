package gg.moonrise.engine.paper.dialog.canvas.internal;

import java.net.URI;
import java.util.Objects;

/**
 * What a clickable canvas element does: run server code, or open a web page in the client.
 * <p>
 * Internal; not part of the supported API.
 * @param run the code to run, or {@code null} for a link
 * @param url the page to open, or {@code null} for code
 */
public record CanvasAction(Runnable run, String url) {

    public CanvasAction {
        if ((run == null) == (url == null)) throw new IllegalArgumentException("A canvas action runs code or opens a page");
    }

    /**
     * Runs code on the player's thread while the screen is current.
     * @param run the code
     * @return the action
     */
    public static CanvasAction run(Runnable run) {
        return new CanvasAction(Objects.requireNonNull(run, "action"), null);
    }

    /**
     * Opens a web page in the viewer's client.
     * @param url an http or https address
     * @return the action
     */
    public static CanvasAction openUrl(String url) {
        Objects.requireNonNull(url, "url");
        String scheme = URI.create(url.trim()).getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Links must be http or https addresses: " + url);
        }

        return new CanvasAction(null, url.trim());
    }

    /**
     * Returns whether this action opens a web page.
     * @return whether it is a link
     */
    public boolean isLink() {
        return url != null;
    }
}
