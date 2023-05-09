package app.cbo.oidc.java.server.backends.ongoingAuths;

import app.cbo.oidc.java.server.datastored.OngoingAuthId;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeParams;
import app.cbo.oidc.java.server.jsr305.NotNull;

@FunctionalInterface
public interface OngoingAuthsStorer {
    @NotNull
    OngoingAuthId store(@NotNull AuthorizeParams p);
}
