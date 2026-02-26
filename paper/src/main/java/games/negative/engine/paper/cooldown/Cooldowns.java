package games.negative.engine.paper.cooldown;

import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import games.negative.moss.spring.SpringComponent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SpringComponent
public class Cooldowns implements Listener {

    private static final Table<UUID, String, Long> COOLDOWNS = Tables.newCustomTable(
            new ConcurrentHashMap<>(),
            ConcurrentHashMap::new
    );

    /**
     * Adds a cooldown for the specified UUID and key.
     * @param uuid UUID of the entity
     * @param key Cooldown key
     * @param millis Duration of the cooldown in milliseconds
     */
    public static void addCooldown(UUID uuid, String key, long millis) {
        long expiryTime = System.currentTimeMillis() + millis;

        COOLDOWNS.put(uuid, key, expiryTime);
    }

    /**
     * Adds a cooldown for the specified UUID and key.
     * @param uuid UUID of the entity
     * @param key Cooldown key
     * @param duration Duration of the cooldown
     */
    public static void addCooldown(UUID uuid, String key, Duration duration) {
        addCooldown(uuid, key, duration.toMillis());
    }

    /**
     * Checks if the specified UUID and key is on cooldown.
     * @param uuid UUID of the entity
     * @param key Cooldown key
     * @return true if on cooldown, false otherwise
     */
    public static boolean isOnCooldown(UUID uuid, String key) {
        return COOLDOWNS.contains(uuid, key) && COOLDOWNS.get(uuid, key) > System.currentTimeMillis();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        COOLDOWNS.row(event.getPlayer().getUniqueId()).clear();
    }

}

