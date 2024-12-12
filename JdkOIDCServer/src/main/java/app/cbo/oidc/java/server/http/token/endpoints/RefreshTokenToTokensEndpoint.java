package app.cbo.oidc.java.server.http.token.endpoints;

import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.http.token.params.RefreshParams;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;

/**
 * Define signature in a funct. interface to simplify unit testing of handler
 */
@FunctionalInterface
public interface RefreshTokenToTokensEndpoint {
    @NotNull
    Interaction treatRequest(@NotNull RefreshParams params, @Nullable ClientId authClientId, @Nullable String clientSecret);
}
