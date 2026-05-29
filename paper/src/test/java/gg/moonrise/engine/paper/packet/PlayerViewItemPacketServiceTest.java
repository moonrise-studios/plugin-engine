package gg.moonrise.engine.paper.packet;

import gg.moonrise.engine.paper.event.PlayerViewItemEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlayerViewItemPacketServiceTest {

    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void reusesCachedClientItemForIdenticalServerItem() {
        ClientItemCache<TestItem> cache = newCache();
        AtomicInteger calls = new AtomicInteger();
        TestItem serverItem = item("server");
        TestItem clientItem = item("client");

        TestItem first = cache.resolve(
                PLAYER_ID,
                PlayerViewItemEvent.Source.SET_SLOT,
                1,
                5,
                serverItem,
                item -> {
                    calls.incrementAndGet();
                    return clientItem;
                }
        );
        TestItem second = cache.resolve(
                PLAYER_ID,
                PlayerViewItemEvent.Source.SET_SLOT,
                1,
                5,
                item("server"),
                item -> {
                    calls.incrementAndGet();
                    return item("unused");
                }
        );

        assertEquals(1, calls.get());
        assertEquals(clientItem, first);
        assertEquals(clientItem, second);
    }

    @Test
    void resolvesAgainWhenServerItemChanges() {
        ClientItemCache<TestItem> cache = newCache();
        AtomicInteger calls = new AtomicInteger();
        TestItem firstServerItem = item("server-1");
        TestItem secondServerItem = item("server-2");
        TestItem secondClientItem = item("client-2");

        cache.resolve(
                PLAYER_ID,
                PlayerViewItemEvent.Source.WINDOW_ITEMS,
                2,
                3,
                firstServerItem,
                item -> {
                    calls.incrementAndGet();
                    return item("client-1");
                }
        );
        TestItem replacement = cache.resolve(
                PLAYER_ID,
                PlayerViewItemEvent.Source.WINDOW_ITEMS,
                2,
                3,
                secondServerItem,
                item -> {
                    calls.incrementAndGet();
                    return secondClientItem;
                }
        );

        assertEquals(2, calls.get());
        assertEquals(secondClientItem, replacement);
    }

    @Test
    void returnsNullWhenClientItemMatchesPacketItem() {
        ClientItemCache<TestItem> cache = newCache();
        AtomicInteger calls = new AtomicInteger();
        TestItem serverItem = item("server");

        TestItem replacement = cache.resolve(
                PLAYER_ID,
                PlayerViewItemEvent.Source.SET_PLAYER_INVENTORY,
                -1,
                8,
                serverItem,
                item -> {
                    calls.incrementAndGet();
                    return null;
                }
        );

        assertEquals(1, calls.get());
        assertNull(replacement);

        TestItem cachedReplacement = cache.resolve(
                PLAYER_ID,
                PlayerViewItemEvent.Source.SET_PLAYER_INVENTORY,
                -1,
                8,
                item("server"),
                item -> {
                    calls.incrementAndGet();
                    return item("unused");
                }
        );

        assertEquals(1, calls.get());
        assertNull(cachedReplacement);
    }

    @Test
    void clearsCachedEntryWhenPacketItemIsEmpty() {
        ClientItemCache<TestItem> cache = newCache();

        cache.resolve(
                PLAYER_ID,
                PlayerViewItemEvent.Source.SET_CURSOR_ITEM,
                -1,
                -1,
                item("server"),
                item -> item("client")
        );
        assertEquals(1, cache.size());

        TestItem replacement = cache.resolve(
                PLAYER_ID,
                PlayerViewItemEvent.Source.SET_CURSOR_ITEM,
                -1,
                -1,
                emptyItem(),
                item -> item("unused")
        );

        assertNull(replacement);
        assertEquals(0, cache.size());
    }

    @Test
    void clearsEntriesForOnePlayer() {
        ClientItemCache<TestItem> cache = newCache();
        UUID otherPlayerId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        cache.resolve(
                PLAYER_ID,
                PlayerViewItemEvent.Source.SET_SLOT,
                1,
                1,
                item("server-1"),
                item -> item("client-1")
        );
        cache.resolve(
                otherPlayerId,
                PlayerViewItemEvent.Source.SET_SLOT,
                1,
                1,
                item("server-2"),
                item -> item("client-2")
        );

        cache.clear(PLAYER_ID);

        assertEquals(1, cache.size());
    }

    private static ClientItemCache<TestItem> newCache() {
        return new ClientItemCache<>(TestItem::empty, item -> item);
    }

    private static TestItem item(String id) {
        return new TestItem(id, false);
    }

    private static TestItem emptyItem() {
        return new TestItem("empty", true);
    }

    private record TestItem(String id, boolean empty) {
    }
}
