package app.cbo.oidc.java.server.backends.claims;

import app.cbo.oidc.java.server.datastored.user.claims.ScopedClaims;

@FunctionalInterface
public interface ClaimsStorer {
    void store(ScopedClaims... someClaims);
}
