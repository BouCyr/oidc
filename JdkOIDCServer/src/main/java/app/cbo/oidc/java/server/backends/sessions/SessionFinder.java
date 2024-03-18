package app.cbo.oidc.java.server.backends.sessions;

import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.jsr305.NotNull;

import java.util.Optional;

/**
 * This interface provides a method to find a session by its SessionId.
 */
public interface SessionFinder {

    /**
     * This method is used to find a session by its SessionId.
     * It returns an Optional that contains the Session if it is found, or an empty Optional if the Session is not found.
     *
     * @param id The SessionId of the session to find.
     * @return An Optional that contains the Session if it is found, or an empty Optional if the Session is not found.
     */
    @NotNull
    Optional<Session> find(@NotNull SessionId id);
}