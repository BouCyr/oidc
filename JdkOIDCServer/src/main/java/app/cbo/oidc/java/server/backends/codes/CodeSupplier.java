package app.cbo.oidc.java.server.backends.codes;

import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Code;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;

import java.util.List;

public interface CodeSupplier {

    /**
     * Generates and 'store' a one time code linked to an authentication request
     *
     * @param userId      userId being authenticated
     * @param clientId    clientId that sent the authenticaiotn request
     * @param sessionId   sessionId of the authenticated user
     * @param redirectUri redirectUri of the authnetication request
     * @param scopes      scopes requested / user info requested by the client
     * @param nonce       nonce provided by the client
     * @return a code, to be consumed by the code endpoint (usually using a server-to-server backchannel)
     */
    @NotNull
    Code createFor(@NotNull UserId userId,
                   @NotNull ClientId clientId,
                   @NotNull SessionId sessionId,
                   @NotNull String redirectUri,
                   @NotNull List<String> scopes,
                   @Nullable String nonce);
}
