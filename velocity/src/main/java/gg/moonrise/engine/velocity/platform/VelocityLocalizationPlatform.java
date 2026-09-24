package gg.moonrise.engine.velocity.platform;

import gg.moonrise.engine.message.LocalizationPlatform;
import gg.moonrise.moss.spring.SpringComponent;
import net.kyori.adventure.audience.Audience;

@SpringComponent
public class VelocityLocalizationPlatform extends LocalizationPlatform {

    public VelocityLocalizationPlatform() {
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
