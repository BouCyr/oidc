package app.cbo.oidc.java.server.http.token;

import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.http.userinfo.ForbiddenResponse;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;

public interface TokenEndpoint {
    @NotNull
    Interaction treatRequest(@NotNull TokenParams params, @Nullable ClientId authClientId, @Nullable String clientSecret) throws JsonError, ForbiddenResponse;

}
