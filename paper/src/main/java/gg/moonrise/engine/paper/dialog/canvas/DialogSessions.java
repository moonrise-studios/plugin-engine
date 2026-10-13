package gg.moonrise.engine.paper.dialog.canvas;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The open canvas session of each player; a player has at most one.
 */
public final class DialogSessions {

    private final ConcurrentHashMap<UUID, DialogSession<?>> sessions = new ConcurrentHashMap<>();

    /**
     * Returns a player's open session.
     * @param playerId the player
     * @return the session, if one is open
     */
    public Optional<DialogSession<?>> active(UUID playerId) {
        DialogSession<?> session = sessions.get(playerId);
        if (session == null) return Optional.empty();
        if (!session.isOpen()) {
            sessions.remove(playerId, session);
            return Optional.empty();
        }

        return Optional.of(session);
    }

    /**
     * Registers a new session, ending the player's previous one with {@link DialogCloseReason#REPLACED}.
     * @param session the new session
     */
    public void register(DialogSession<?> session) {
        Objects.requireNonNull(session, "session");
        DialogSession<?> previous = sessions.put(session.playerId(), session);
        if (previous != null && previous != session) previous.dispose(DialogCloseReason.REPLACED);
    }

    /**
     * Removes a session if it is still the player's registered one.
     * @param session the session
     */
    public void remove(DialogSession<?> session) {
        sessions.remove(session.playerId(), session);
    }

    /**
     * Closes every session that outlived its timeout. Run periodically by the registered runtime.
     */
    public void sweep() {
        for (DialogSession<?> session : sessions.values()) {
            session.isOpen();
        }
    }

    /**
     * Ends every session without touching screens, for example when the plugin disables.
     * @param reason why the sessions end
     */
    public void disposeAll(DialogCloseReason reason) {
        for (DialogSession<?> session : sessions.values()) {
            session.dispose(reason);
        }
        sessions.clear();
    }

    /**
     * Ends a player's session without touching their screen.
     * @param playerId the player
     * @param reason why the session ends
     */
    public void dispose(UUID playerId, DialogCloseReason reason) {
        DialogSession<?> session = sessions.remove(playerId);
        if (session != null) session.dispose(reason);
    }
}
