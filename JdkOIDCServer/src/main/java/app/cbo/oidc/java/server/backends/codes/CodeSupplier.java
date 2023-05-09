package app.cbo.oidc.java.server.backends.codes;

import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Code;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;

import java.util.List;

public interface CodeSupplier {
    @NotNull
    Code createFor(@NotNull UserId userId,
                   @NotNull ClientId clientId,
                   @NotNull SessionId sessionId,
                   @NotNull String redirectUri,
                   @NotNull List<String> scopes,
                   @Nullable String nonce);
}
