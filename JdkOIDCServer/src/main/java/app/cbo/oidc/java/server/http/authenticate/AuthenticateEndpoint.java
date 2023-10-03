package app.cbo.oidc.java.server.http.authenticate;

import app.cbo.oidc.java.server.http.AuthErrorInteraction;
import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;

import java.util.Collection;
import java.util.Map;

@FunctionalInterface
public interface AuthenticateEndpoint {
    @NotNull
    Interaction treatRequest(@NotNull Map<String, Collection<String>> rawParams) throws AuthErrorInteraction;
}
