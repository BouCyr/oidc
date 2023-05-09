package app.cbo.oidc.java.server.backends.sessions;

import app.cbo.oidc.java.server.credentials.AuthenticationMode;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.datastored.user.User;
import app.cbo.oidc.java.server.jsr305.NotNull;

import java.util.EnumSet;

public interface SessionSupplier {
    @NotNull
    SessionId createSession(@NotNull User user, @NotNull EnumSet<AuthenticationMode> authenticationModes);
}
