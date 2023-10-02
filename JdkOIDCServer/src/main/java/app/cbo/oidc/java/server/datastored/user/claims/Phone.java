package app.cbo.oidc.java.server.datastored.user.claims;

import app.cbo.oidc.java.server.datastored.user.UserId;

public record Phone(UserId userId, String phone, boolean phone_verified) implements ScopedClaims {
    @Override
    public String scopeName() {
        return "phone";
    }
}
