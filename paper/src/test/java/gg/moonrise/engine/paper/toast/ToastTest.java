package gg.moonrise.engine.paper.toast;

import gg.moonrise.engine.message.Message;
import gg.moonrise.engine.paper.support.MockBukkitTest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToastTest extends MockBukkitTest {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    @Test
    void appliesBuilderDefaults() {
        Toast toast = Toast.builder().title("Hello").build();

        assertEquals(ToastFrame.TASK, toast.getFrame());
        assertEquals(ToastJavaLine.TITLE, toast.getJavaLine());
        assertEquals(Material.PAPER, toast.getIcon().getType());
        assertFalse(toast.hasContent());
    }

    @Test
    void honoursExplicitBuilderValues() {
        Toast toast = Toast.builder()
                .title("Hello")
                .content("World")
                .icon(Material.DIAMOND)
                .frame(ToastFrame.CHALLENGE)
                .javaLine(ToastJavaLine.BOTH)
                .build();

        assertEquals(ToastFrame.CHALLENGE, toast.getFrame());
        assertEquals(ToastJavaLine.BOTH, toast.getJavaLine());
        assertEquals(Material.DIAMOND, toast.getIcon().getType());
        assertTrue(toast.hasContent());
    }

    @Test
    void copiesIconSoToastStaysImmutable() {
        ItemStack icon = new ItemStack(Material.DIAMOND);
        Toast toast = Toast.builder().title("Hello").icon(icon).build();

        icon.setType(Material.DIRT);

        assertEquals(Material.DIAMOND, toast.getIcon().getType());
    }

    @Test
    void returnsDefensiveIconCopy() {
        Toast toast = Toast.builder().title("Hello").icon(Material.DIAMOND).build();

        ItemStack returned = toast.getIcon();
        returned.setType(Material.DIRT);

        assertEquals(Material.DIAMOND, toast.getIcon().getType());
    }

    @Test
    void rejectsToastWithoutTitle() {
        Toast.Builder builder = Toast.builder().content("World");

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void rejectsNullBuilderValues() {
        Toast.Builder builder = Toast.builder();

        assertThrows(NullPointerException.class, () -> builder.title((String) null));
        assertThrows(NullPointerException.class, () -> builder.title((Component) null));
        assertThrows(NullPointerException.class, () -> builder.title((Message) null));
        assertThrows(NullPointerException.class, () -> builder.icon((Material) null));
        assertThrows(NullPointerException.class, () -> builder.icon((ItemStack) null));
        assertThrows(NullPointerException.class, () -> builder.frame(null));
        assertThrows(NullPointerException.class, () -> builder.javaLine(null));
    }

    @Test
    void clearsContentWithNull() {
        Toast toast = Toast.builder().title("Hello").content("World").content((String) null).build();

        assertFalse(toast.hasContent());
        assertNull(toast.content(server.addPlayer("Eric")));
    }

    @Test
    void resolvesJavaLineForTitle() {
        PlayerMock player = server.addPlayer("Eric");
        Toast toast = Toast.builder().title("Hello").content("World").build();

        assertEquals("Hello", PLAIN.serialize(toast.javaComponent(player)));
    }

    @Test
    void resolvesJavaLineForContent() {
        PlayerMock player = server.addPlayer("Eric");
        Toast toast = Toast.builder()
                .title("Hello")
                .content("World")
                .javaLine(ToastJavaLine.CONTENT)
                .build();

        assertEquals("World", PLAIN.serialize(toast.javaComponent(player)));
    }

    @Test
    void resolvesJavaLineForBoth() {
        PlayerMock player = server.addPlayer("Eric");
        Toast toast = Toast.builder()
                .title("Hello")
                .content("World")
                .javaLine(ToastJavaLine.BOTH)
                .build();

        assertEquals("Hello World", PLAIN.serialize(toast.javaComponent(player)));
    }

    @Test
    void fallsBackToTitleWhenContentIsMissing() {
        PlayerMock player = server.addPlayer("Eric");

        Toast contentLine = Toast.builder().title("Hello").javaLine(ToastJavaLine.CONTENT).build();
        Toast bothLines = Toast.builder().title("Hello").javaLine(ToastJavaLine.BOTH).build();

        assertEquals("Hello", PLAIN.serialize(contentLine.javaComponent(player)));
        assertEquals("Hello", PLAIN.serialize(bothLines.javaComponent(player)));
    }

    @Test
    void resolvesMessagesPerViewer() {
        PlayerMock player = server.addPlayer("Eric");
        Toast toast = Toast.builder().title(Message.of("<green>Hello")).build();

        assertEquals("Hello", PLAIN.serialize(toast.title(player)));
    }

    @Test
    void serializesBedrockTextAsLegacySection() {
        PlayerMock player = server.addPlayer("Eric");
        Toast toast = Toast.builder()
                .title(Component.text("Hello", NamedTextColor.GREEN))
                .content(Component.text("World", NamedTextColor.RED))
                .build();

        assertEquals("§aHello", toast.bedrockTitle(player));
        assertEquals("§cWorld", toast.bedrockContent(player));
    }

    @Test
    void serializesMissingBedrockContentAsEmptyString() {
        PlayerMock player = server.addPlayer("Eric");
        Toast toast = Toast.builder().title("Hello").build();

        assertEquals("", toast.bedrockContent(player));
    }

    @Test
    void serializesMiniMessageTitleForBedrock() {
        PlayerMock player = server.addPlayer("Eric");
        Toast toast = Toast.builder().title("<green>Hello").build();

        assertEquals("§aHello", toast.bedrockTitle(player));
    }
}
