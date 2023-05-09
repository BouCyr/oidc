package app.cbo.oidc.java.server.backends.codes;

import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Code;
import app.cbo.oidc.java.server.datastored.CodeData;
import app.cbo.oidc.java.server.jsr305.NotNull;

import java.util.Optional;

public interface CodeConsumer {
    @NotNull
    Optional<CodeData> consume(@NotNull Code code, @NotNull ClientId clientId, @NotNull String redirectUri);
}
