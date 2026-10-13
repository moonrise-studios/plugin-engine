package gg.moonrise.engine.paper.dialog.canvas;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache of player faces used by canvas head components.
 * <p>
 * Rendering never downloads anything: an uncached face renders as {@link HeadPixels#placeholder()} and
 * starts a background fetch. Skins are only downloaded from {@code textures.minecraft.net}; faces from
 * other skin sources can be supplied with {@link #put}. Preload faces in {@link DialogSession#load} so the first render already has them.
 */
public final class PlayerHeads {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerHeads.class);
    private static final Duration TIME_TO_LIVE = Duration.ofMinutes(30);
    private static final Duration FAILURE_TIME_TO_LIVE = Duration.ofMinutes(1);
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(10);
    private static final String SKIN_HOST = "textures.minecraft.net";
    private static final int EVICTION_THRESHOLD = 1024;

    private final DialogExecutor executor;
    private final ConcurrentHashMap<UUID, Entry> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<HeadPixels>> inFlight = new ConcurrentHashMap<>();
    private volatile HttpClient httpClient;

    public PlayerHeads(DialogExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    /**
     * Returns a cached face that has not expired.
     * @param playerId the player
     * @return the face, if cached
     */
    public Optional<HeadPixels> cached(UUID playerId) {
        Entry entry = cache.get(playerId);
        if (entry == null || entry.expiresAt() < System.currentTimeMillis()) return Optional.empty();

        return Optional.of(entry.pixels());
    }

    /**
     * Returns a cached face, or the placeholder while a background fetch runs.
     * @param playerId the player
     * @return the face to render now
     */
    public HeadPixels cachedOrPlaceholder(UUID playerId) {
        Optional<HeadPixels> cached = cached(playerId);
        if (cached.isPresent()) return cached.get();

        fetch(playerId);
        return HeadPixels.placeholder();
    }

    /**
     * Stores a face, for example one produced from a custom skin source.
     * @param playerId the player
     * @param pixels the face
     */
    public void put(UUID playerId, HeadPixels pixels) {
        store(playerId, Objects.requireNonNull(pixels, "pixels"), TIME_TO_LIVE.toMillis());
    }

    public void invalidate(UUID playerId) {
        cache.remove(playerId);
    }

    /**
     * Fetches a face in the background, reusing the cache and any fetch already running.
     * <p>
     * Failures are logged and resolve to the placeholder, which is cached briefly.
     * @param playerId the player
     * @return the face
     */
    public CompletableFuture<HeadPixels> fetch(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId");
        Optional<HeadPixels> cached = cached(playerId);
        if (cached.isPresent()) return CompletableFuture.completedFuture(cached.get());

        CompletableFuture<HeadPixels> future = new CompletableFuture<>();
        CompletableFuture<HeadPixels> running = inFlight.putIfAbsent(playerId, future);
        if (running != null) return running;

        executor.async(() -> {
            HeadPixels pixels = HeadPixels.placeholder();
            long ttl = FAILURE_TIME_TO_LIVE.toMillis();
            try {
                pixels = download(playerId);
                ttl = TIME_TO_LIVE.toMillis();
            } catch (Throwable t) {
                if (t instanceof InterruptedException) Thread.currentThread().interrupt();
                LOGGER.warn("Failed to load skin for {}: {}", playerId, t.toString());
            } finally {
                store(playerId, pixels, ttl);
                inFlight.remove(playerId, future);
                future.complete(pixels);
            }
        });
        return future;
    }

    /**
     * Fetches several faces in the background.
     * @param playerIds the players
     * @return completes once every face is cached
     */
    public CompletableFuture<Void> preload(Collection<UUID> playerIds) {
        return CompletableFuture.allOf(playerIds.stream()
                .map(this::fetch)
                .toArray(CompletableFuture[]::new));
    }

    private void store(UUID playerId, HeadPixels pixels, long ttl) {
        long now = System.currentTimeMillis();
        if (cache.size() >= EVICTION_THRESHOLD) cache.values().removeIf(entry -> entry.expiresAt() < now);

        cache.put(playerId, new Entry(pixels, now + ttl));
    }

    private HeadPixels download(UUID playerId) throws IOException, InterruptedException {
        Player online = Bukkit.getPlayer(playerId);
        PlayerProfile profile = online != null ? online.getPlayerProfile() : Bukkit.createProfile(playerId);
        if (!profile.hasTextures()) profile.complete(true);

        URL skin = profile.getTextures().getSkin();
        if (skin == null) return HeadPixels.placeholder();

        URI uri = URI.create(skin.toString());
        if (!SKIN_HOST.equalsIgnoreCase(uri.getHost()) || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))) {
            throw new IOException("Refusing to download a skin from " + uri.getHost());
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(DOWNLOAD_TIMEOUT)
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient().send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) throw new IOException("Skin server returned HTTP " + response.statusCode());

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(response.body()));
        if (image == null) throw new IOException("Skin is not a readable image");

        return HeadPixels.fromSkin(image);
    }

    private HttpClient httpClient() {
        HttpClient client = httpClient;
        if (client != null) return client;

        synchronized (this) {
            if (httpClient == null) {
                httpClient = HttpClient.newBuilder()
                        .connectTimeout(DOWNLOAD_TIMEOUT)
                        .followRedirects(HttpClient.Redirect.NEVER)
                        .build();
            }
            return httpClient;
        }
    }

    private record Entry(HeadPixels pixels, long expiresAt) {
    }
}
