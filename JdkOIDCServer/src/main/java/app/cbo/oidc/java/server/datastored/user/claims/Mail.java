package app.cbo.oidc.java.server.datastored.user.claims;

import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.oidc.Constants;

public record Mail(UserId userId, String email, boolean email_verified) implements ScopedClaims {

    @Override
    public String scopeName() {
        return Constants.Scope.MAIL;
    }
}
