package app.cbo.oidc.java.server.http.consent;

import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.http.AuthErrorInteraction;
import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;

import java.util.Optional;

public interface ConsentEndpoint {
    @NotNull
    Interaction treatRequest(
            @NotNull Optional<Session> maybeSession,
            @NotNull ConsentParams params) throws AuthErrorInteraction;
}
