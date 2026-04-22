package gg.moonrise.engine.paper.platform;

import gg.moonrise.engine.message.LocalizationPlatform;
import gg.moonrise.moss.spring.SpringComponent;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.audience.Audience;
import org.bukkit.entity.Player;

@SpringComponent
public class PaperLocalizationPlatform extends LocalizationPlatform {

    public PaperLocalizationPlatform() {
        setInstance(this);
    }

    @Override
    public <T extends Audience> String parsePlaceholders(T recipient, String text) {
        if (!(recipient instanceof Player player)) {
            return PlaceholderAPI.setPlaceholders(null, text);
        }

        return PlaceholderAPI.setPlaceholders(
                player,
                text
        );
    }

    @Override
    public <T extends Audience> String parseRelationalPlaceholders(T origin, T viewer, String text) {
        Player originPlayer = origin instanceof Player player ? player : null;
        Player viewerPlayer = viewer instanceof Player player ? player : null;

        return PlaceholderAPI.setRelationalPlaceholders(
                originPlayer,
                viewerPlayer,
                text
        );
    }
}
