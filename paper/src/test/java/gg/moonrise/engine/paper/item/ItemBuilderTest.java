package gg.moonrise.engine.paper.item;

import gg.moonrise.engine.paper.support.MockBukkitTest;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemBuilderTest extends MockBukkitTest {

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    @Test
    void buildsMaterialStackWithMeta() {
        NamespacedKey key = new NamespacedKey("plugin_engine", "test");

        ItemStack stack = ItemBuilder.of(Material.DIAMOND)
                .amount(3)
                .name("<green>Gem")
                .lore(List.of("<gray>Line one", "<yellow>Line two"))
                .customModelData(1234)
                .applyPersistentData(data -> data.set(key, PersistentDataType.STRING, "value"))
                .build();

        ItemMeta meta = stack.getItemMeta();
        assertEquals(Material.DIAMOND, stack.getType());
        assertEquals(3, stack.getAmount());
        assertEquals("Gem", PLAIN.serialize(meta.displayName()));
        assertEquals(List.of("Line one", "Line two"), meta.lore().stream().map(PLAIN::serialize).toList());
        assertEquals(1234, meta.getCustomModelData());
        assertEquals("value", meta.getPersistentDataContainer().get(key, PersistentDataType.STRING));
    }

    @Test
    void buildReturnsClone() {
        ItemBuilder builder = ItemBuilder.of(Material.STONE).amount(2);

        ItemStack first = builder.build();
        ItemStack second = builder.build();
        first.setAmount(9);

        assertNotSame(first, second);
        assertEquals(9, first.getAmount());
        assertEquals(2, second.getAmount());
    }

    @Test
    void copiesExistingBuilderState() {
        ItemBuilder original = ItemBuilder.of(Material.EMERALD).amount(5);

        ItemStack copied = ItemBuilder.of(original).build();

        assertEquals(Material.EMERALD, copied.getType());
        assertEquals(5, copied.getAmount());
        assertTrue(copied.hasItemMeta());
    }
}
