package app.cbo.oidc.java.server.datastored.user.claims;

import app.cbo.oidc.java.server.datastored.user.UserId;

public interface ScopedClaims {

    UserId userId();

    String scopeName();

}
