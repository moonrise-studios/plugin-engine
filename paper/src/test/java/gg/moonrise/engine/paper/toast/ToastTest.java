package gg.moonrise.engine.paper.toast;

import gg.moonrise.engine.message.Message;
import gg.moonrise.engine.paper.support.MockBukkitTest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextDecoration;
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

public class ToastTest extends MockBukkitTest {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    @Test
    public void appliesBuilderDefaults() {
        Toast toast = Toast.builder().title("Hello").build();

        assertEquals(ToastFrame.TASK, toast.getFrame());
        assertEquals(ToastJavaLine.TITLE, toast.getJavaLine());
        assertEquals(Material.PAPER, toast.getIcon().getType());
        assertFalse(toast.hasContent());
    }

    @Test
    public void honoursExplicitBuilderValues() {
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
    public void copiesIconSoToastStaysImmutable() {
        ItemStack icon = new ItemStack(Material.DIAMOND);
        Toast toast = Toast.builder().title("Hello").icon(icon).build();

        icon.setType(Material.DIRT);

        assertEquals(Material.DIAMOND, toast.getIcon().getType());
    }

    @Test
    public void returnsDefensiveIconCopy() {
        Toast toast = Toast.builder().title("Hello").icon(Material.DIAMOND).build();

        ItemStack returned = toast.getIcon();
        returned.setType(Material.DIRT);

        assertEquals(Material.DIAMOND, toast.getIcon().getType());
    }

    @Test
    public void rejectsToastWithoutTitle() {
        Toast.Builder builder = Toast.builder().content("World");

        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    public void rejectsNullBuilderValues() {
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
    public void clearsContentWithNull() {
        Toast toast = Toast.builder().title("Hello").content("World").content((String) null).build();

        assertFalse(toast.hasContent());
        assertNull(toast.content(server.addPlayer("Eric")));
    }

    @Test
    public void resolvesJavaLineForTitle() {
        PlayerMock player = server.addPlayer("Eric");
        Toast toast = Toast.builder().title("Hello").content("World").build();

        assertEquals("Hello", PLAIN.serialize(toast.javaComponent(player)));
    }

    @Test
    public void resolvesJavaLineForContent() {
        PlayerMock player = server.addPlayer("Eric");
        Toast toast = Toast.builder()
                .title("Hello")
                .content("World")
                .javaLine(ToastJavaLine.CONTENT)
                .build();

        assertEquals("World", PLAIN.serialize(toast.javaComponent(player)));
    }

    @Test
    public void resolvesJavaLineForBoth() {
        PlayerMock player = server.addPlayer("Eric");
        Toast toast = Toast.builder()
                .title("Hello")
                .content("World")
                .javaLine(ToastJavaLine.BOTH)
                .build();

        assertEquals("Hello World", PLAIN.serialize(toast.javaComponent(player)));
    }

    @Test
    public void resolvesJavaLineForTwoLines() {
        PlayerMock player = server.addPlayer("Eric");
        Toast toast = Toast.builder()
                .title("Title")
                .content("Content")
                .javaLine(ToastJavaLine.TWO_LINES)
                .build();

        Component resolved = toast.javaComponent(player);

        assertEquals("Title\nContent", PLAIN.serialize(resolved));
        assertTrue(containsNewlineComponent(resolved), "the component tree must contain a literal newline component");
    }

    @Test
    public void fallsBackToTitleOnlyForTwoLinesWithoutContent() {
        PlayerMock player = server.addPlayer("Eric");
        Toast toast = Toast.builder().title("Title").javaLine(ToastJavaLine.TWO_LINES).build();

        Component resolved = toast.javaComponent(player);

        assertEquals("Title", PLAIN.serialize(resolved));
        assertFalse(containsNewlineComponent(resolved));
    }

    @Test
    public void doesNotLeakTitleStyleIntoContentForTwoLines() {
        PlayerMock player = server.addPlayer("Eric");
        Toast toast = Toast.builder()
                .title(Component.text("Title", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .content(Component.text("Content", NamedTextColor.WHITE))
                .javaLine(ToastJavaLine.TWO_LINES)
                .build();

        Component resolved = toast.javaComponent(player);
        Component content = resolved.children().get(2);

        assertEquals(Style.empty(), resolved.style(), "the joining parent must be unstyled");
        assertEquals("Content", PLAIN.serialize(content));
        assertEquals(NamedTextColor.WHITE, content.color());
        assertEquals(TextDecoration.State.NOT_SET, content.decoration(TextDecoration.BOLD));
    }

    @Test
    public void doesNotLeakTitleStyleIntoContentForBoth() {
        PlayerMock player = server.addPlayer("Eric");
        Toast toast = Toast.builder()
                .title(Component.text("Title", NamedTextColor.GREEN).decorate(TextDecoration.BOLD))
                .content(Component.text("Content", NamedTextColor.WHITE))
                .javaLine(ToastJavaLine.BOTH)
                .build();

        Component resolved = toast.javaComponent(player);
        Component content = resolved.children().get(2);

        assertEquals(Style.empty(), resolved.style(), "the joining parent must be unstyled");
        assertEquals(NamedTextColor.WHITE, content.color());
        assertEquals(TextDecoration.State.NOT_SET, content.decoration(TextDecoration.BOLD));
    }

    @Test
    public void keepsBedrockTextIndependentOfJavaLine() {
        PlayerMock player = server.addPlayer("Eric");

        for (ToastJavaLine javaLine : ToastJavaLine.values()) {
            Toast toast = Toast.builder().title("Title").content("Content").javaLine(javaLine).build();

            assertEquals("Title", toast.bedrockTitle(player));
            assertEquals("Content", toast.bedrockContent(player));
        }
    }

    @Test
    public void fallsBackToTitleWhenContentIsMissing() {
        PlayerMock player = server.addPlayer("Eric");

        Toast contentLine = Toast.builder().title("Hello").javaLine(ToastJavaLine.CONTENT).build();
        Toast bothLines = Toast.builder().title("Hello").javaLine(ToastJavaLine.BOTH).build();

        assertEquals("Hello", PLAIN.serialize(contentLine.javaComponent(player)));
        assertEquals("Hello", PLAIN.serialize(bothLines.javaComponent(player)));
    }

    @Test
    public void resolvesMessagesPerViewer() {
        PlayerMock player = server.addPlayer("Eric");
        Toast toast = Toast.builder().title(Message.of("<green>Hello")).build();

        assertEquals("Hello", PLAIN.serialize(toast.title(player)));
    }

    @Test
    public void serializesBedrockTextAsLegacySection() {
        PlayerMock player = server.addPlayer("Eric");
        Toast toast = Toast.builder()
                .title(Component.text("Hello", NamedTextColor.GREEN))
                .content(Component.text("World", NamedTextColor.RED))
                .build();

        assertEquals("§aHello", toast.bedrockTitle(player));
        assertEquals("§cWorld", toast.bedrockContent(player));
    }

    @Test
    public void serializesMissingBedrockContentAsEmptyString() {
        PlayerMock player = server.addPlayer("Eric");
        Toast toast = Toast.builder().title("Hello").build();

        assertEquals("", toast.bedrockContent(player));
    }

    private static boolean containsNewlineComponent(Component component) {
        if (component instanceof TextComponent text && text.content().contains("\n")) return true;

        return component.children().stream().anyMatch(ToastTest::containsNewlineComponent);
    }

    @Test
    public void serializesMiniMessageTitleForBedrock() {
        PlayerMock player = server.addPlayer("Eric");
        Toast toast = Toast.builder().title("<green>Hello").build();

        assertEquals("§aHello", toast.bedrockTitle(player));
    }
}
