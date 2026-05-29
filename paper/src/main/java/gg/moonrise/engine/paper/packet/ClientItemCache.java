package gg.moonrise.engine.paper.packet;

import gg.moonrise.engine.paper.event.PlayerViewItemEvent;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

final class ClientItemCache<T> {

    private final Map<ClientItemKey, CachedClientItem<T>> entries = new ConcurrentHashMap<>();
    private final Predicate<T> emptyItem;
    private final UnaryOperator<T> copyItem;

    ClientItemCache(Predicate<T> emptyItem, UnaryOperator<T> copyItem) {
        this.emptyItem = emptyItem;
        this.copyItem = copyItem;
    }

    T resolve(
            UUID playerId,
            PlayerViewItemEvent.Source source,
            int windowId,
            int slot,
            T packetItem,
            Function<T, T> resolver
    ) {
        ClientItemKey key = new ClientItemKey(playerId, source, windowId, slot);

        if (packetItem == null || emptyItem.test(packetItem)) {
            entries.remove(key);
            return null;
        }

        CachedClientItem<T> cached = entries.get(key);
        if (cached != null && Objects.equals(cached.serverItem(), packetItem)) {
            return replacement(packetItem, cached.clientItem());
        }

        T clientItem = resolver.apply(packetItem);
        if (clientItem == null) clientItem = packetItem;

        entries.put(key, new CachedClientItem<>(copyItem.apply(packetItem), copyItem.apply(clientItem)));
        return replacement(packetItem, clientItem);
    }

    void clear() {
        entries.clear();
    }

    void clear(UUID playerId) {
        entries.keySet().removeIf(key -> key.playerId().equals(playerId));
    }

    int size() {
        return entries.size();
    }

    private T replacement(T packetItem, T clientItem) {
        if (Objects.equals(packetItem, clientItem)) return null;
        return copyItem.apply(clientItem);
    }

    private record ClientItemKey(UUID playerId, PlayerViewItemEvent.Source source, int windowId, int slot) {
    }

    private record CachedClientItem<T>(T serverItem, T clientItem) {
    }
}
