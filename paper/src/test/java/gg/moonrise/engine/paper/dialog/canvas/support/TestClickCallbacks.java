package gg.moonrise.engine.paper.dialog.canvas.support;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Test stand-in for Paper's click callback provider: stores callbacks and lets tests invoke them.
 */
public final class TestClickCallbacks implements ClickCallback.Provider {

    private static final String PREFIX = "/test:callback ";
    private static final AtomicLong IDS = new AtomicLong();
    private static final Map<String, ClickCallback<Audience>> CALLBACKS = new ConcurrentHashMap<>();

    @Override
    public ClickEvent create(ClickCallback<Audience> callback, ClickCallback.Options options) {
        String id = Long.toString(IDS.incrementAndGet());
        CALLBACKS.put(id, callback);
        return ClickEvent.runCommand(PREFIX + id);
    }

    /**
     * Runs the callback behind a click event.
     * @param event the click event
     * @param audience who clicked
     */
    public static void click(ClickEvent event, Audience audience) {
        if (!(event.payload() instanceof ClickEvent.Payload.Text text) || !text.value().startsWith(PREFIX)) {
            throw new IllegalArgumentException("Not a test callback: " + event);
        }

        ClickCallback<Audience> callback = CALLBACKS.get(text.value().substring(PREFIX.length()));
        if (callback == null) throw new IllegalStateException("Unknown callback " + text.value());

        callback.accept(audience);
    }

    /**
     * Finds the click events attached to text components with exactly the given content.
     * @param root the component tree to search
     * @param content the text content
     * @return the click events, in tree order
     */
    public static List<ClickEvent> clicksOnText(Component root, String content) {
        List<ClickEvent> clicks = new ArrayList<>();
        collect(root, content, clicks);
        return clicks;
    }

    private static void collect(Component component, String content, List<ClickEvent> clicks) {
        if (component instanceof TextComponent text
                && text.content().equals(content)
                && text.clickEvent() != null) {
            clicks.add(text.clickEvent());
        }

        for (Component child : component.children()) {
            collect(child, content, clicks);
        }
    }
}
