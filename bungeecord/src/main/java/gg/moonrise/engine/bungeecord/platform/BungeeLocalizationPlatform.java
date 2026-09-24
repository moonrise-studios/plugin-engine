package gg.moonrise.engine.bungeecord.platform;

import gg.moonrise.engine.message.LocalizationPlatform;
import gg.moonrise.moss.spring.SpringComponent;
import net.kyori.adventure.audience.Audience;

@SpringComponent
public class BungeeLocalizationPlatform extends LocalizationPlatform {

    public BungeeLocalizationPlatform() {
        setInstance(this);
    }

    @Override
    public <T extends Audience> String parsePlaceholders(T recipient, String text) {
        return text;
    }

    @Override
    public <T extends Audience> String parseRelationalPlaceholders(T origin, T viewer, String text) {
        return text;
    }
}
