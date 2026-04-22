package gg.moonrise.engine.message.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents the LegacyTranslator class.
 */

public class LegacyTranslator {

    private static final Map<Character, String> LEGACY_COLOR_MAP = new HashMap<>();
    private static final Map<Character, String> LEGACY_FORMAT_MAP = new HashMap<>();

    // Pattern for ultra-legacy Minecraft hex: (§|& )x followed by 6 (§|&)nibbles
    // Example: §x§C§C§C§C§F§F or &x&c&c&c&c&f&f
    private static final Pattern ULTRA_LEGACY_HEX_PATTERN = Pattern.compile(
            "([&§])x(?:\\1([0-9A-Fa-f]))(?:\\1([0-9A-Fa-f]))(?:\\1([0-9A-Fa-f]))(?:\\1([0-9A-Fa-f]))(?:\\1([0-9A-Fa-f]))(?:\\1([0-9A-Fa-f]))"
    );

    // Pattern for legacy color/format codes (& or §)
    private static final Pattern LEGACY_PATTERN = Pattern.compile("([&§])([0-9a-fk-or])");

    // Pattern for hex colors: &#RRGGBB or §#RRGGBB
    private static final Pattern HEX_PATTERN = Pattern.compile("([&§])#([0-9a-fA-F]{6})");

    static {
        // Color codes
        LEGACY_COLOR_MAP.put('0', "black");
        LEGACY_COLOR_MAP.put('1', "dark_blue");
        LEGACY_COLOR_MAP.put('2', "dark_green");
        LEGACY_COLOR_MAP.put('3', "dark_aqua");
        LEGACY_COLOR_MAP.put('4', "dark_red");
        LEGACY_COLOR_MAP.put('5', "dark_purple");
        LEGACY_COLOR_MAP.put('6', "gold");
        LEGACY_COLOR_MAP.put('7', "gray");
        LEGACY_COLOR_MAP.put('8', "dark_gray");
        LEGACY_COLOR_MAP.put('9', "blue");
        LEGACY_COLOR_MAP.put('a', "green");
        LEGACY_COLOR_MAP.put('b', "aqua");
        LEGACY_COLOR_MAP.put('c', "red");
        LEGACY_COLOR_MAP.put('d', "light_purple");
        LEGACY_COLOR_MAP.put('e', "yellow");
        LEGACY_COLOR_MAP.put('f', "white");

        // Format codes
        LEGACY_FORMAT_MAP.put('k', "obfuscated");
        LEGACY_FORMAT_MAP.put('l', "bold");
        LEGACY_FORMAT_MAP.put('m', "strikethrough");
        LEGACY_FORMAT_MAP.put('n', "underlined");
        LEGACY_FORMAT_MAP.put('o', "italic");
        LEGACY_FORMAT_MAP.put('r', "reset");
    }

    /**
     * Translates legacy color codes to MiniMessage format.
     *
     * @param input The input string with legacy codes
     * @return The translated MiniMessage string
     */
    public static String translate(String input) {
        if (input == null || input.isEmpty()) return input;

        String result = input;

        result = translateHexColors(result);
        result = translateUltraLegacyHexColors(result);
        result = translateLegacyCodes(result);

        return result;
    }

    private static String translateUltraLegacyHexColors(String input) {
        if (input == null || input.isEmpty()) return input;

        Matcher matcher = ULTRA_LEGACY_HEX_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            // Groups 2..7 are the six nibbles
            String rr = matcher.group(2) + matcher.group(3);
            String gg = matcher.group(4) + matcher.group(5);
            String bb = matcher.group(6) + matcher.group(7);
            String hex = (rr + gg + bb).toUpperCase();

            String replacement = "<#" + hex + ">";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Translates hex color codes (&#RRGGBB or §#RRGGBB) to MiniMessage format.
     */
    private static String translateHexColors(String input) {
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String hexColor = matcher.group(2);
            String replacement = "<#" + hexColor + ">";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Translates legacy color and format codes to MiniMessage format.
     */
    private static String translateLegacyCodes(String input) {
        Matcher matcher = LEGACY_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            char code = matcher.group(2).toLowerCase().charAt(0);
            String replacement;

            if (LEGACY_COLOR_MAP.containsKey(code)) {
                replacement = "<" + LEGACY_COLOR_MAP.get(code) + ">";
            } else if (LEGACY_FORMAT_MAP.containsKey(code)) {
                replacement = "<" + LEGACY_FORMAT_MAP.get(code) + ">";
            } else {
                // If code not recognized, keep original
                replacement = matcher.group(0);
            }

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

}
