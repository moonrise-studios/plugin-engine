package gg.moonrise.engine.paper.dialog.canvas;

import gg.moonrise.engine.paper.dialog.canvas.internal.TextMeasurer;
import gg.moonrise.engine.paper.dialog.canvas.theme.DialogTheme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DialogLayoutsTest {

    @Test
    public void groupsEntriesIntoPyramidRows() {
        List<Integer> entries = IntStream.rangeClosed(1, 16).boxed().toList();

        List<List<Integer>> rows = DialogLayouts.rows(entries, 1, 3, 5, 7);

        assertEquals(List.of(1, 3, 5, 7), rows.stream().map(List::size).toList());
        assertEquals(List.of(1), rows.get(0));
        assertEquals(List.of(10, 11, 12, 13, 14, 15, 16), rows.get(3));
    }

    @Test
    public void stopsEarlyAndRepeatsTheLastSize() {
        assertEquals(List.of(1, 2), DialogLayouts.rows(List.of(1, 2, 3), 1, 3).stream().map(List::size).toList());
        assertEquals(List.of(1, 3, 3), DialogLayouts.rows(List.of(1, 2, 3, 4, 5, 6, 7), 1, 3).stream().map(List::size).toList());
        assertThrows(IllegalArgumentException.class, () -> DialogLayouts.rows(List.of(1), 0));
    }

    @Test
    public void measuresVanillaAdvances() {
        TextMeasurer measurer = new TextMeasurer(DialogTheme.builder("moonrise", "test").build().compiled());

        assertEquals(24, measurer.width(Component.text("Hello")));
        assertEquals(29, measurer.width(Component.text("Hello").decorate(TextDecoration.BOLD)));
        assertEquals(31, measurer.width(Component.text("He").append(Component.text("llo ").decorate(TextDecoration.BOLD))));
    }

    @Test
    public void truncatesAndWrapsToWidth() {
        TextMeasurer measurer = new TextMeasurer(DialogTheme.builder("moonrise", "test").build().compiled());

        Component truncated = measurer.truncate(Component.text("A very long player name"), 40);
        assertTrue(measurer.width(truncated) <= 40);

        List<Component> lines = measurer.wrap(Component.text("one two three four five six seven eight"), 50);
        assertTrue(lines.size() > 1);
        for (Component line : lines) {
            assertTrue(measurer.width(line) <= 50);
        }
    }

    @Test
    public void gridLeavesTheRestInALastRow() {
        List<List<Integer>> rows = DialogLayouts.grid(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9), 2);

        assertEquals(List.of(2, 2, 2, 2, 1), rows.stream().map(List::size).toList());
        assertEquals(List.of(9), rows.get(4));
    }

    @Test
    public void wrappingKeepsInlineStyles() {
        TextMeasurer measurer = new TextMeasurer(DialogTheme.builder("moonrise", "test").build().compiled());
        Component text = Component.text("Hello ")
                .append(Component.text("NegativeKB", NamedTextColor.YELLOW))
                .append(Component.text(", you have plenty of claim blocks left to spend on land"));

        List<Component> lines = measurer.wrap(text, 80);

        assertTrue(lines.size() > 1);
        for (Component line : lines) {
            assertTrue(measurer.width(line) <= 80, PlainTextComponentSerializer.plainText().serialize(line));
        }
        assertTrue(lines.stream().flatMap(line -> line.children().stream()).anyMatch(part -> part instanceof TextComponent segment
                && segment.content().startsWith("NegativeKB") && NamedTextColor.YELLOW.equals(segment.color())));
        String joined = String.join(" ", lines.stream().map(PlainTextComponentSerializer.plainText()::serialize).toList());
        assertEquals("Hello NegativeKB, you have plenty of claim blocks left to spend on land", joined);
    }

    @Test
    public void wrappingBreaksAtLineBreaksAndOverlongWords() {
        TextMeasurer measurer = new TextMeasurer(DialogTheme.builder("moonrise", "test").build().compiled());

        List<Component> lines = measurer.wrap(Component.text("one\n\nAAAAAAAAAAAAAAAAAAAA"), 40);

        assertEquals("one", PlainTextComponentSerializer.plainText().serialize(lines.get(0)));
        assertEquals("", PlainTextComponentSerializer.plainText().serialize(lines.get(1)));
        assertTrue(lines.size() > 3);
        for (Component line : lines) assertTrue(measurer.width(line) <= 40);
    }

    @Test
    public void wrappingKeepsTranslatablePartsWhole() {
        TextMeasurer measurer = new TextMeasurer(DialogTheme.builder("moonrise", "test").build().compiled());
        Component text = Component.text("You found a ")
                .append(Component.translatable("item.minecraft.diamond_pickaxe", NamedTextColor.AQUA))
                .append(Component.text(" in the chest of the abandoned mineshaft"));

        List<Component> lines = measurer.wrap(text, 90);

        assertTrue(lines.size() > 1);
        assertTrue(lines.stream().flatMap(line -> line.children().stream())
                .anyMatch(part -> part instanceof TranslatableComponent translatable
                        && translatable.key().equals("item.minecraft.diamond_pickaxe")
                        && NamedTextColor.AQUA.equals(translatable.color())));
    }
}
