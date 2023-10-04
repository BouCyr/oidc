package app.cbo.oidc.java.server.http.token;

import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.http.userinfo.ForbiddenResponse;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;
import app.cbo.oidc.java.server.utils.Utils;

public interface TokenEndpoint {
    @NotNull
    Interaction treatRequest(@NotNull TokenParams params, @Nullable String authClientId, @Nullable String clientSecret) throws JsonError, ForbiddenResponse;

    default boolean authenticateClient(String clientId, String clientSecret) {
        return !Utils.isEmpty(clientId) && clientId.equals(clientSecret);//TODO [14/04/2023] client registry ?
    }
}
