package app.cbo.oidc.java.server.backends.sessions;

import app.cbo.oidc.java.server.credentials.AuthenticationMode;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.datastored.user.User;
import app.cbo.oidc.java.server.jsr305.NotNull;

import java.util.EnumSet;

/**
 * This interface provides a method to create a new session for a user with a set of authentication modes.
 */
public interface SessionSupplier {

    /**
     * This method is used to create a new session for a user with a set of authentication modes.
     * It returns the SessionId of the created session.
     *
     * @param user The User for whom the session is to be created.
     * @param authenticationModes The set of AuthenticationModes to be used for the session.
     * @return The SessionId of the created session.
     */
    @NotNull
    SessionId createSession(@NotNull User user, @NotNull EnumSet<AuthenticationMode> authenticationModes);
}
