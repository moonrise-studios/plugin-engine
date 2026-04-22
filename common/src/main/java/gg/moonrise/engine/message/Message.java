package gg.moonrise.engine.message;

import gg.moonrise.engine.message.util.MiniMessageUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Represents a message that can be sent to an audience, with support for MiniMessage formatting and placeholders.
 * @param content the content of the message
 */
public record Message(String content) {

    /**
     * Creates a Message object from a single string.
     * @param content the content of the message
     * @return a Message object containing the content
     */
    public static Message of(String content) {
        return new Message(content);
    }

    /**
     * Creates a Message object from multiple lines of strings, joining them with the <newline> delimiter.
     * @param lines the lines of strings to join
     * @return a Message object containing the joined content
     */
    public static Message of(String... lines) {
        return new Message(String.join("<newline>", lines));
    }

    /**
     * Creates a Message object from a list of strings, joining them with the <newline> delimiter.
     * @param lines the list of strings to join
     * @return a Message object containing the joined content
     */
    public static Message of(List<String> lines) {
        return new Message(String.join("<newline>", lines));
    }

    /**
     * Sends the message to the specified recipient, replacing any placeholders if provided.
     * @param recipient the audience to send the message to
     * @param placeholders placeholders to replace in the message
     * @param <T> the type of the audience
     */
    public <T extends Audience> void send(@NotNull T recipient, TagResolver.Single... placeholders) {
        recipient.sendMessage(asComponent(recipient, placeholders));
    }

    /**
     * Converts the message content into a Component, replacing any placeholders if provided.
     * @param placeholders placeholders to replace in the message
     * @return the resulting Component
     */
    public Component asComponent(TagResolver.Single... placeholders) {
        return asComponent(null, placeholders);
    }

    /**
     * Converts the message content into a Component for a specific viewer, replacing any placeholders if provided.
     * @param viewer the audience viewing the message
     * @param placeholders placeholders to replace in the message
     * @param <T> the type of the audience
     * @return the resulting Component
     */
    public <T extends Audience> Component asComponent(T viewer, TagResolver.Single... placeholders) {
        return MiniMessageUtil.fromText(viewer, content, placeholders);
    }

    /**
     * Converts the message content into a Component with relational placeholders, using the origin as the sender and the viewer as the recipient, replacing any placeholders if provided.
     * @param origin the audience that is the sender of the placeholders
     * @param viewer the audience that is the recipient of the placeholders
     * @param placeholders placeholders to replace in the message
     * @return the resulting Component
     * @param <T> the type of the audience
     */
    public <T extends Audience> Component asRelationalComponent(T origin, T viewer, TagResolver.Single... placeholders) {
        return MiniMessageUtil.fromRelationalText(origin, viewer, content, placeholders);
    }
}
