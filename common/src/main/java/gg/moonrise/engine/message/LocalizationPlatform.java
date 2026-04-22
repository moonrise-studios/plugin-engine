package gg.moonrise.engine.message;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.audience.Audience;

/**
 * Represents a platform-specific implementation for localization and placeholder parsing.
 * This abstract class defines the contract for parsing placeholders in text based on the recipient's context, allowing for different implementations depending on the platform (e.g., Bukkit, Velocity, etc.).
 * Subclasses should implement the methods to provide platform-specific logic for parsing placeholders and relational placeholders.
 */
public abstract class LocalizationPlatform {

    @Getter
    @Setter(AccessLevel.PROTECTED)
    private static LocalizationPlatform instance;

    /**
     * Parses placeholders in the given text for the specified recipient. This method should be implemented by subclasses to provide platform-specific placeholder parsing logic. The implementation may use a placeholder API or custom logic to replace placeholders in the text based on the recipient's context.
     * @param recipient the audience for whom the placeholders should be parsed, which can be used to determine the context for placeholder replacement (e.g., player-specific placeholders)
     * @param text the text containing the placeholders to parse, which may include placeholders that need to be replaced with actual values based on the recipient's context
     * @return the text with the placeholders parsed and replaced with their corresponding values based on the recipient's context
     * @param <T> the type of the audience, which allows for flexibility in handling different types of recipients (e.g., players, console, etc.) when parsing placeholders
     */
    public abstract <T extends Audience> String parsePlaceholders(T recipient, String text);

    /**
     * Parses relational placeholders in the given text for the specified origin and viewer. This method should be implemented by subclasses to provide platform-specific relational placeholder parsing logic. The implementation may use a placeholder API or custom logic to replace relational placeholders in the text based on the context of both the origin and viewer.
     * @param origin the audience that is the sender of the placeholders, which can be used to determine the context for placeholder replacement from the perspective of the sender (e.g., player-specific placeholders related to the sender)
     * @param viewer the audience that is the recipient of the placeholders, which can be used to determine the context for placeholder replacement from the perspective of the recipient (e.g., player-specific placeholders related to the recipient)
     * @param text the text containing the relational placeholders to parse, which may include placeholders that need to be replaced with actual values based on the context of both the origin and viewer
     * @return the text with the relational placeholders parsed and replaced with their corresponding values based on the context of both the origin and viewer
     * @param <T> the type of the audience, which allows for flexibility in handling different types of recipients (e.g., players, console, etc.) when parsing relational placeholders
     */
    public abstract <T extends Audience> String parseRelationalPlaceholders(T origin, T viewer, String text);

}
