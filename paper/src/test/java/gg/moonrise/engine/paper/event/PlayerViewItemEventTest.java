package gg.moonrise.engine.paper.event;

import gg.moonrise.engine.paper.support.MockBukkitTest;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerViewItemEventTest extends MockBukkitTest {

    @Test
    void exposesConstructorValues() {
        Player player = server.addPlayer("Viewer");
        ItemStack item = new ItemStack(Material.DIAMOND, 2);

        PlayerViewItemEvent event = new PlayerViewItemEvent(
                player,
                item,
                PlayerViewItemEvent.Source.SET_SLOT,
                1,
                5,
                9,
                true
        );

        assertEquals(player, event.getPlayer());
        assertEquals(item, event.getItemStack());
        assertEquals(item, event.getOriginalItemStack());
        assertEquals(PlayerViewItemEvent.Source.SET_SLOT, event.getSource());
        assertEquals(1, event.getWindowId());
        assertEquals(5, event.getSlot());
        assertEquals(9, event.getStateId());
        assertTrue(event.isAsynchronous());
    }

    @Test
    void clonesOriginalAndCurrentItems() {
        Player player = server.addPlayer("Viewer");
        ItemStack original = new ItemStack(Material.EMERALD);

        PlayerViewItemEvent event = new PlayerViewItemEvent(
                player,
                original,
                PlayerViewItemEvent.Source.WINDOW_ITEMS,
                2,
                3,
                4,
                false
        );

        original.setAmount(32);

        ItemStack eventItem = event.getItemStack();
        ItemStack originalItem = event.getOriginalItemStack();

        assertNotNull(eventItem);
        assertNotNull(originalItem);
        assertEquals(1, eventItem.getAmount());
        assertEquals(1, originalItem.getAmount());
        assertNotSame(eventItem, originalItem);

        eventItem.setAmount(16);
        assertEquals(16, event.getItemStack().getAmount());
        assertEquals(1, event.getOriginalItemStack().getAmount());
    }

    @Test
    void replacesClientItemWithoutChangingOriginalItem() {
        Player player = server.addPlayer("Viewer");
        ItemStack original = new ItemStack(Material.STONE);
        ItemStack replacement = new ItemStack(Material.DIAMOND);

        PlayerViewItemEvent event = new PlayerViewItemEvent(
                player,
                original,
                PlayerViewItemEvent.Source.SET_PLAYER_INVENTORY,
                -1,
                8,
                -1,
                false
        );

        event.setItemStack(replacement);
        replacement.setAmount(7);

        assertEquals(new ItemStack(Material.STONE), event.getOriginalItemStack());
        assertEquals(new ItemStack(Material.DIAMOND), event.getItemStack());
    }

    @Test
    void supportsClearingClientItem() {
        Player player = server.addPlayer("Viewer");
        PlayerViewItemEvent event = new PlayerViewItemEvent(
                player,
                new ItemStack(Material.STONE),
                PlayerViewItemEvent.Source.SET_CURSOR_ITEM,
                -1,
                -1,
                -1,
                false
        );

        event.setItemStack(null);

        assertNull(event.getItemStack());
        assertEquals(new ItemStack(Material.STONE), event.getOriginalItemStack());
    }

    @Test
    void exposesHandlerList() {
        assertNotNull(PlayerViewItemEvent.getHandlerList());

        PlayerViewItemEvent event = new PlayerViewItemEvent(
                server.addPlayer("Viewer"),
                new ItemStack(Material.STONE),
                PlayerViewItemEvent.Source.WINDOW_CARRIED_ITEM,
                -1,
                -1,
                -1,
                false
        );

        assertEquals(PlayerViewItemEvent.getHandlerList(), event.getHandlers());
    }
}
