package app.cbo.oidc.java.server.backends.claims;

import app.cbo.oidc.java.server.datastored.user.claims.ScopedClaims;

@FunctionalInterface
public interface ClaimsStorer {

    /**
     * Store (on disk) some info for a user.
     *
     * @param someClaims info to be stored, as defined in a 'standard' oidc claim
     */
    void store(ScopedClaims... someClaims);
}
