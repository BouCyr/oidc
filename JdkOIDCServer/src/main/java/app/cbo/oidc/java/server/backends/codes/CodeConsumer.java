package app.cbo.oidc.java.server.backends.codes;

import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Code;
import app.cbo.oidc.java.server.datastored.CodeData;
import app.cbo.oidc.java.server.jsr305.NotNull;

import java.util.Optional;

public interface CodeConsumer {

    /**
     * Checks, consumes a code, returning the needed data
     *
     * @param code        the code being received by the server for validation
     * @param clientId    the client id that sent the code back
     * @param redirectUri the redirectUri sent with the validation
     * @return the data stored server-side for this code at generation (userId, sessionId, scopes requested and nonce) ; EMPTY if the code is invalid, or not recognized by the server
     */
    @NotNull
    Optional<CodeData> consume(@NotNull Code code, @NotNull ClientId clientId, @NotNull String redirectUri);
}
