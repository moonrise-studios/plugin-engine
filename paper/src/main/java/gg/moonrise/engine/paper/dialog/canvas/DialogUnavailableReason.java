package gg.moonrise.engine.paper.dialog.canvas;

/**
 * Why a canvas dialog could not be shown to a player.
 */
public enum DialogUnavailableReason {
    /**
     * The client has not applied a pack containing the theme's glyphs and the menu does not fall back
     * to a vanilla dialog, see {@link DialogMenu#nativeFallback}.
     */
    RESOURCE_PACK_NOT_LOADED("<red>This menu needs the server resource pack. Accept it and try again."),
    /**
     * The client is older than Minecraft 1.21.6 and cannot show dialogs.
     */
    UNSUPPORTED_CLIENT("<red>This menu needs Minecraft 1.21.6 or newer."),
    /**
     * The player is on Bedrock Edition and the menu does not offer a form, see {@link DialogMenu#bedrockForms}.
     */
    BEDROCK_CLIENT("<red>This menu is not available on Bedrock Edition.");

    private final String defaultMessage;

    private DialogUnavailableReason(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    /**
     * MiniMessage text sent by {@link DialogMenu#onUnavailable} unless overridden.
     * @return the message
     */
    public String defaultMessage() {
        return defaultMessage;
    }
}
