package app.cbo.oidc.java.server.backends.ongoingAuths;

import app.cbo.oidc.java.server.datastored.OngoingAuthId;
import app.cbo.oidc.java.server.http.authorize.AuthorizeParams;
import app.cbo.oidc.java.server.jsr305.NotNull;

import java.util.Optional;

/**
 * This interface provides a way to retrieve ongoing authentications.

 */
@FunctionalInterface
public interface OngoingAuthsFinder {

    /**
     * This method returns the ongoing authentication associated with the provided OngoingAuthId.
     * @param key The OngoingAuthId of the ongoing authentication to retrieve.
     * @return the ongoing authentication associated with the provided OngoingAuthId, or an empty Optional if no ongoing authentication is found.
     */
    @NotNull
    Optional<AuthorizeParams> find(@NotNull OngoingAuthId key);
}
