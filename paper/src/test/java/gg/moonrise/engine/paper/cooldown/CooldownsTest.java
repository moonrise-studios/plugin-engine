package gg.moonrise.engine.paper.cooldown;

import gg.moonrise.engine.paper.support.MockBukkitTest;
import net.kyori.adventure.text.Component;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerQuitEvent.QuitReason;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CooldownsTest extends MockBukkitTest {

    @Test
    void cooldownsExpireByTime() {
        PlayerMock player = server.addPlayer();
        String key = "action";

        Cooldowns.addCooldown(player.getUniqueId(), key, Duration.ofSeconds(1));
        assertTrue(Cooldowns.isOnCooldown(player.getUniqueId(), key));

        Cooldowns.addCooldown(player.getUniqueId(), key, -1L);
        assertFalse(Cooldowns.isOnCooldown(player.getUniqueId(), key));
    }

    @Test
    void quitClearsPlayerCooldowns() {
        PlayerMock player = server.addPlayer();
        String key = "quit-action";
        Cooldowns.addCooldown(player.getUniqueId(), key, Duration.ofMinutes(1));

        new Cooldowns().onQuit(new PlayerQuitEvent(player, Component.empty(), QuitReason.DISCONNECTED));

        assertFalse(Cooldowns.isOnCooldown(player.getUniqueId(), key));
    }

    @Test
    void cooldownCanBeRemovedByKey() {
        PlayerMock player = server.addPlayer();
        String key = "removable-action";
        Cooldowns.addCooldown(player.getUniqueId(), key, Duration.ofMinutes(1));

        Cooldowns.removeCooldown(player.getUniqueId(), key);

        assertFalse(Cooldowns.isOnCooldown(player.getUniqueId(), key));
    }
}
