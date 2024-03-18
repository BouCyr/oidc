package app.cbo.oidc.java.server.backends.ongoingAuths;

import app.cbo.oidc.java.server.datastored.OngoingAuthId;
import app.cbo.oidc.java.server.http.authorize.AuthorizeParams;
import app.cbo.oidc.java.server.jsr305.NotNull;

/**
 * This interface provides a way to store ongoing authentications.

 */
@FunctionalInterface
public interface OngoingAuthsStorer {

    /**
     * This method stores the provided ongoing authentication and returns the OngoingAuthId associated with it.
     * @param p The ongoing authentication to store.
     * @return the OngoingAuthId associated with the stored ongoing authentication.
     */
    @NotNull
    OngoingAuthId store(@NotNull AuthorizeParams p);
}
