package gg.moonrise.engine.paper.toast;

/**
 * The outcome of a toast delivery attempt.
 */
public enum ToastResult {

    /**
     * The toast was delivered through the Geyser API as a native Bedrock toast.
     */
    BEDROCK,

    /**
     * The toast was delivered to a Java Edition client as a transient fake advancement.
     */
    JAVA,

    /**
     * No delivery route was available, so nothing was sent.
     */
    UNSUPPORTED
}
