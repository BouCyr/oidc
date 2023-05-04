package app.cbo.oidc.java.server.datastored.user.claims;

import app.cbo.oidc.java.server.datastored.user.UserId;

public record Address(UserId userId, String formatted, String street_address, String locality, String region,
                      String postal_code, String country) implements ScopedClaims {

}
