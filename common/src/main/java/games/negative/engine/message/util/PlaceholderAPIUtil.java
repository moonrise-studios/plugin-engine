package games.negative.engine.message.util;

import games.negative.engine.message.LocalizationPlatform;
import net.kyori.adventure.audience.Audience;

/**
 * Represents the PlaceholderAPIUtil class.
 */

public final class PlaceholderAPIUtil {

    private static final boolean ENABLED = isPlaceholderAPIAvailable();

    private static boolean isPlaceholderAPIAvailable() {
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Executes parsePlaceholders.
     * @param recipient the recipient
     * @param text the text
     * @return the result
     */

    public static <T extends Audience> String parsePlaceholders(T recipient, String text) {
        if (!ENABLED) return text;

        LocalizationPlatform instance = LocalizationPlatform.getInstance();
        if (instance == null) return text;

        return instance.parsePlaceholders(recipient, text);
    }

    /**
     * Parses relational placeholders in the given text using the origin as the sender and the viewer as the recipient.
     * @param origin the audience that is the sender of the placeholders
     * @param viewer the audience that is the recipient of the placeholders
     * @param text the text containing the placeholders to parse
     * @return the text with the relational placeholders parsed
     * @param <T> the type of the audience
     */
    public static <T extends Audience> String parseRelationalPlaceholders(T origin, T viewer, String text) {
        if (!ENABLED) return text;

        LocalizationPlatform instance = LocalizationPlatform.getInstance();
        if (instance == null) return text;

        return instance.parseRelationalPlaceholders(origin, viewer, text);
    }

}
