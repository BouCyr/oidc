package app.cbo.oidc.java.server.backends.tokens;

import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;

public interface AccessTokenGenerator {

    String generate(
            @NotNull ClientId requestedBy,
            @NotNull Session activeSession,
            @Nullable String resource);
}
