package gg.moonrise.engine.paper.toast;

import gg.moonrise.engine.message.Message;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Objects;

/**
 * An immutable, platform-agnostic toast notification.
 * <p>
 * The same toast renders as a native Bedrock toast for Geyser players and as a
 * transient fake advancement for Java Edition players. Send one with
 * {@link Toasts#send(Player, Toast)}.
 */
public final class Toast {

    /**
     * The icon used when no explicit icon is supplied.
     */
    public static final Material DEFAULT_ICON = Material.PAPER;

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final ToastText title;
    private final ToastText content;
    private final ItemStack icon;

    /**
     * The advancement frame used on Java Edition.
     */
    @Getter
    private final ToastFrame frame;

    /**
     * Which part of the toast is rendered on the single customizable Java Edition line.
     */
    @Getter
    private final ToastJavaLine javaLine;

    private Toast(Builder builder) {
        this.title = builder.title;
        this.content = builder.content;
        this.icon = builder.icon == null ? new ItemStack(DEFAULT_ICON) : builder.icon.clone();
        this.frame = builder.frame;
        this.javaLine = builder.javaLine;
    }

    /**
     * Creates a new toast builder.
     * @return a fresh builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The icon shown next to the toast on Java Edition. Bedrock toasts have no icon.
     * @return a defensive copy of the icon
     */
    public ItemStack getIcon() {
        return icon.clone();
    }

    /**
     * Resolves the toast title for a viewer.
     * @param viewer the player the toast is rendered for
     * @return the resolved title, never {@code null}
     */
    public Component title(Player viewer) {
        return title.resolve(viewer);
    }

    /**
     * Resolves the toast content for a viewer.
     * @param viewer the player the toast is rendered for
     * @return the resolved content, or {@code null} when the toast has no content
     */
    public Component content(Player viewer) {
        return content == null ? null : content.resolve(viewer);
    }

    /**
     * Checks whether this toast carries content in addition to its title.
     * @return {@code true} when content is present
     */
    public boolean hasContent() {
        return content != null;
    }

    /**
     * Resolves the single line shown on a Java Edition toast, honouring the configured
     * {@link ToastJavaLine} and falling back to the other part when the selected one is absent.
     * @param viewer the player the toast is rendered for
     * @return the component rendered as the advancement title
     */
    public Component javaComponent(Player viewer) {
        Component titleComponent = title(viewer);
        Component contentComponent = content(viewer);

        return switch (javaLine) {
            case TITLE -> titleComponent;
            case CONTENT -> contentComponent == null ? titleComponent : contentComponent;
            case BOTH -> contentComponent == null
                    ? titleComponent
                    : titleComponent.append(Component.space()).append(contentComponent);
        };
    }

    /**
     * Serializes the title for Bedrock clients, which only accept legacy section formatting.
     * @param viewer the player the toast is rendered for
     * @return the legacy-formatted title
     */
    public String bedrockTitle(Player viewer) {
        return legacy(title(viewer));
    }

    /**
     * Serializes the content for Bedrock clients, which only accept legacy section formatting.
     * @param viewer the player the toast is rendered for
     * @return the legacy-formatted content, or an empty string when the toast has no content
     */
    public String bedrockContent(Player viewer) {
        Component contentComponent = content(viewer);
        return contentComponent == null ? "" : legacy(contentComponent);
    }

    static String legacy(Component component) {
        return LEGACY.serialize(component);
    }

    /**
     * Fluent builder for {@link Toast}.
     */
    public static final class Builder {

        private ToastText title;
        private ToastText content;
        private ItemStack icon;
        private ToastFrame frame = ToastFrame.TASK;
        private ToastJavaLine javaLine = ToastJavaLine.TITLE;

        private Builder() {
        }

        /**
         * Sets the title from a MiniMessage string.
         * @param title the MiniMessage title
         * @return this builder
         */
        public Builder title(String title) {
            this.title = ToastText.of(title);
            return this;
        }

        /**
         * Sets the title from a component.
         * @param title the title component
         * @return this builder
         */
        public Builder title(Component title) {
            this.title = ToastText.of(title);
            return this;
        }

        /**
         * Sets the title from a message, resolved per viewer at send time.
         * @param title the title message
         * @return this builder
         */
        public Builder title(Message title) {
            this.title = ToastText.of(title);
            return this;
        }

        /**
         * Sets the content from a MiniMessage string.
         * @param content the MiniMessage content, or {@code null} to clear it
         * @return this builder
         */
        public Builder content(String content) {
            this.content = ToastText.nullable(content);
            return this;
        }

        /**
         * Sets the content from a component.
         * @param content the content component, or {@code null} to clear it
         * @return this builder
         */
        public Builder content(Component content) {
            this.content = ToastText.nullable(content);
            return this;
        }

        /**
         * Sets the content from a message, resolved per viewer at send time.
         * @param content the content message, or {@code null} to clear it
         * @return this builder
         */
        public Builder content(Message content) {
            this.content = ToastText.nullable(content);
            return this;
        }

        /**
         * Sets the Java Edition icon.
         * @param icon the icon item
         * @return this builder
         */
        public Builder icon(ItemStack icon) {
            this.icon = Objects.requireNonNull(icon, "icon");
            return this;
        }

        /**
         * Sets the Java Edition icon from a material.
         * @param icon the icon material
         * @return this builder
         */
        public Builder icon(Material icon) {
            Objects.requireNonNull(icon, "icon");
            this.icon = new ItemStack(icon);
            return this;
        }

        /**
         * Sets the Java Edition advancement frame.
         * @param frame the frame
         * @return this builder
         */
        public Builder frame(ToastFrame frame) {
            this.frame = Objects.requireNonNull(frame, "frame");
            return this;
        }

        /**
         * Sets which part of the toast is rendered on the Java Edition line.
         * @param javaLine the line selection
         * @return this builder
         */
        public Builder javaLine(ToastJavaLine javaLine) {
            this.javaLine = Objects.requireNonNull(javaLine, "javaLine");
            return this;
        }

        /**
         * Builds the toast.
         * @return the immutable toast
         * @throws IllegalStateException when no title was supplied
         */
        public Toast build() {
            if (title == null) throw new IllegalStateException("A toast requires a title");

            return new Toast(this);
        }
    }
}
