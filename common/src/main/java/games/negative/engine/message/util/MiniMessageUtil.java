package games.negative.engine.message.util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Optional;

/**
 * Represents the MiniMessageUtil class.
 */

public class MiniMessageUtil {

    public static MiniMessage INSTANCE = MiniMessage.miniMessage();

    private static final LegacyComponentSerializer ULTRA_LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .useUnusualXRepeatedCharacterHexFormat()
            .hexCharacter(LegacyComponentSerializer.HEX_CHAR)
            .hexColors()
            .build();

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.AMPERSAND_CHAR)
            .useUnusualXRepeatedCharacterHexFormat()
            .hexCharacter(LegacyComponentSerializer.HEX_CHAR)
            .hexColors()
            .build();

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    /**
     * Initializes the MiniMessage instance with the provided placeholders.
     * @param placeholders an array of TagResolver.Single to be used as placeholders
     */
    public static void init(TagResolver.Single... placeholders) {
        if (placeholders == null || placeholders.length == 0) {
            INSTANCE = MiniMessage.miniMessage();
            return;
        }

        INSTANCE = MiniMessage.builder().editTags(builder -> builder.resolvers(placeholders)).build();
    }

    /**
     * Converts a text string into a Component, applying PlaceholderAPI placeholders and MiniMessage formatting.
     * @param text the text to convert
     * @param placeholders placeholders to replace in the text
     * @return the resulting Component
     */
    public static Component fromText(String text, TagResolver.Single...placeholders) {
        return fromText((Audience) null, text, placeholders);
    }

    /**
     * Converts a text string into a Component using a specific MiniMessage instance, applying MiniMessage formatting.
     * @param provider the MiniMessage instance to use
     * @param text the text to convert
     * @param placeholders placeholders to replace in the text
     * @return the resulting Component
     */
    public static Component fromText(MiniMessage provider, String text, TagResolver.Single... placeholders) {
        return fromText(resolveProvider(provider), null, text, placeholders);
    }

    /**
     * Converts a text string into a Component for a specific recipient, applying PlaceholderAPI placeholders and MiniMessage formatting.
     * @param recipient the audience receiving the message
     * @param text the text to convert
     * @param placeholders placeholders to replace in the text
     * @return the resulting Component
     * @param <T> the type of the audience
     */
    public static <T extends Audience> Component fromText(T recipient, String text, TagResolver.Single... placeholders) {
        return fromText(resolveProvider(INSTANCE), recipient, text, placeholders);
    }

    /**
     * Converts a text string into a Component using a specific MiniMessage instance for a specific recipient,
     * applying PlaceholderAPI placeholders and MiniMessage formatting.
     * @param miniMessage the MiniMessage instance to use
     * @param recipient the audience receiving the message
     * @param text the text to convert
     * @param placeholders placeholders to replace in the text
     * @return the resulting Component
     * @param <T> the type of the audience
     */
    public static <T extends Audience> Component fromText(MiniMessage miniMessage, T recipient, String text, TagResolver.Single... placeholders) {
        miniMessage = resolveProvider(miniMessage);

        // Apply PlaceholderAPI placeholders
        text = PlaceholderAPIUtil.parsePlaceholders(recipient, text);

        // Replace legacy color codes with MiniMessage format
        if (text.contains("&") || text.contains("§")) text = LegacyTranslator.translate(text);

        String result = text;
        MiniMessage finalMiniMessage = miniMessage;
        return checkPlaceholders(placeholders)
                .map(resolvers -> finalMiniMessage.deserialize(result, resolvers))
                .orElseGet(() -> finalMiniMessage.deserialize(result));
    }

    /**
     * Converts a text string into a Component for a specific origin and viewer, applying PlaceholderAPI placeholders, relational placeholders, and MiniMessage formatting.
     * @param provider the MiniMessage instance to use
     * @param origin the audience that is the sender of the placeholders
     * @param viewer the audience that is the recipient of the placeholders
     * @param text the text to convert
     * @param placeholders placeholders to replace in the text
     * @return the resulting Component
     * @param <T> the type of the audience
     */
    public static <T extends Audience> Component fromRelationalText(MiniMessage provider, T origin, T viewer, String text, TagResolver.Single[] placeholders) {
        provider = resolveProvider(provider);

        // Apply PlaceholderAPI placeholders
        text = PlaceholderAPIUtil.parsePlaceholders(origin, text);
        text = PlaceholderAPIUtil.parseRelationalPlaceholders(origin, viewer, text);

        // Replace legacy color codes with MiniMessage format
        if (text.contains("&") || text.contains("§")) text = LegacyTranslator.translate(text);

        String result = text;
        MiniMessage finalProvider = provider;
        return checkPlaceholders(placeholders)
                .map(resolvers -> finalProvider.deserialize(result, resolvers))
                .orElseGet(() -> finalProvider.deserialize(result));
    }

    /**
     * Converts a text string into a Component for a specific viewer, applying PlaceholderAPI placeholders, relational placeholders, and MiniMessage formatting.
     * @param origin the audience that is the sender of the placeholders
     * @param viewer the audience that is the recipient of the placeholders
     * @param text the text to convert
     * @param placeholders placeholders to replace in the text
     * @return the resulting Component
     * @param <T> the type of the audience
     */
    public static <T extends Audience> Component fromRelationalText(T origin, T viewer, String text, TagResolver.Single[] placeholders) {
        return fromRelationalText(resolveProvider(INSTANCE), origin, viewer, text, placeholders);
    }

    /**
     * Checks if the provided placeholders array is null or empty, and returns an Optional containing the array if it has elements, or an empty Optional if it is null or empty.
     * @param placeholders the array of TagResolver.Single placeholders to check
     * @return an Optional containing the placeholders array if it is not null and has elements, or an empty Optional if it is null or empty
     */
    private static Optional<TagResolver.Single[]> checkPlaceholders(TagResolver.Single... placeholders) {
        return (placeholders == null || placeholders.length == 0 ? Optional.empty() : Optional.of(placeholders));
    }

    /**
     * Converts a Component to its legacy string representation. Primarily only used for PlaceholderAPI.
     * @param component the Component to convert
     * @return the legacy string representation
     */
    public static String componentToLegacy(Component component) {
        return LEGACY_SERIALIZER.serialize(component);
    }

    /**
     * Converts a legacy string representation to a Component. Primarily only used for PlaceholderAPI.
     * @param text the legacy string to convert
     * @return the resulting Component
     */
    public static Component legacyToComponent(String text) {
        return LEGACY_SERIALIZER.deserialize(text);
    }

    /**
     * Converts a Component to its MiniMessage string representation.
     * @param component the Component to convert
     * @return the MiniMessage string representation
     */
    public static String componentToMiniMessage(Component component) {
        return resolveProvider(INSTANCE).serialize(component);
    }

    /**
     * Converts a MiniMessage string representation to a Component.
     * @param text the MiniMessage string to convert
     * @return the resulting Component
     */
    public static Component miniMessageToComponent(String text) {
        return resolveProvider(INSTANCE).deserialize(text);
    }

    /**
     * Converts a Component to its plain text representation.
     * @param component the Component to convert
     * @return the plain text representation
     */
    public static String componentToPlainText(Component component) {
        return PLAIN.serialize(component);
    }

    /**
     * Converts a plain text representation to a Component.
     * @param text the plain text to convert
     * @return the resulting Component
     */
    public static Component toPlainTextComponent(String text) {
        return PLAIN.deserialize(text);
    }

    /**
     * Converts a legacy string representation to its MiniMessage string representation.
     * @param text the legacy string to convert
     * @return the MiniMessage string representation
     */
    public static String legacyToMiniMessage(String text) {
        TextComponent deserialize = ULTRA_LEGACY_SERIALIZER.deserialize(text);
        text = LEGACY_SERIALIZER.serialize(deserialize);

        Component component = LEGACY_SERIALIZER.deserialize(text);
        return resolveProvider(INSTANCE).serialize(component);
    }

    /**
     * Executes fromLegacy.
     * @param recipient the recipient
     * @param text the text
     * @return the result
     */

    public static <T extends Audience> Component fromLegacy(T recipient, String text) {
        // Apply PlaceholderAPI placeholders
        text = PlaceholderAPIUtil.parsePlaceholders(recipient, text);

        TextComponent deserialize = ULTRA_LEGACY_SERIALIZER.deserialize(text);
        text = LEGACY_SERIALIZER.serialize(deserialize);

        return LEGACY_SERIALIZER.deserialize(text);
    }

    private static MiniMessage resolveProvider(MiniMessage provider) {
        return provider == null ? MiniMessage.miniMessage() : provider;
    }
}
