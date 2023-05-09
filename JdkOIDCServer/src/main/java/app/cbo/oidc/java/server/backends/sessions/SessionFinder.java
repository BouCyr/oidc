package app.cbo.oidc.java.server.backends.sessions;

import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.jsr305.NotNull;

import java.util.Optional;

public interface SessionFinder {
    @NotNull
    Optional<Session> find(@NotNull SessionId id);
}
