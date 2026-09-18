package gg.moonrise.engine.paper.toast;

import gg.moonrise.engine.message.Message;
import gg.moonrise.engine.paper.support.MockBukkitTest;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ToastsTest extends MockBukkitTest {

    @Test
    public void returnsUnsupportedWhenNoRouteIsAvailable() {
        PlayerMock player = server.addPlayer("Eric");
        Toast toast = Toast.builder().title("Hello").build();

        assertEquals(ToastResult.UNSUPPORTED, Toasts.send(player, toast));
    }

    @Test
    public void returnsUnsupportedForEveryConvenienceOverload() {
        PlayerMock player = server.addPlayer("Eric");

        assertEquals(ToastResult.UNSUPPORTED, Toasts.send(player, "Hello"));
        assertEquals(ToastResult.UNSUPPORTED, Toasts.send(player, "Hello", "World"));
        assertEquals(ToastResult.UNSUPPORTED, Toasts.send(player, Component.text("Hello")));
        assertEquals(ToastResult.UNSUPPORTED, Toasts.send(player, Component.text("Hello"), Component.text("World")));
        assertEquals(ToastResult.UNSUPPORTED, Toasts.send(player, Message.of("Hello")));
        assertEquals(ToastResult.UNSUPPORTED, Toasts.send(player, Message.of("Hello"), Message.of("World")));
    }

    @Test
    public void rejectsNullArguments() {
        PlayerMock player = server.addPlayer("Eric");
        Toast toast = Toast.builder().title("Hello").build();

        assertThrows(NullPointerException.class, () -> Toasts.send(null, toast));
        assertThrows(NullPointerException.class, () -> Toasts.send(player, (Toast) null));
    }

    @Test
    public void exposesBuilderFromFacade() {
        assertNotNull(Toasts.builder().title("Hello").build());
    }
}
