package gg.moonrise.engine.paper.toast;

/**
 * The advancement frame used to render a toast on Java Edition clients.
 * <p>
 * The client derives the first line of the toast ("Advancement Made!", "Goal Reached!",
 * "Challenge Complete!") from this frame, so it cannot be customized any further.
 */
public enum ToastFrame {

    /**
     * Renders as "Advancement Made!".
     */
    TASK,

    /**
     * Renders as "Goal Reached!".
     */
    GOAL,

    /**
     * Renders as "Challenge Complete!".
     */
    CHALLENGE
}
