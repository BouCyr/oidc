package app.cbo.oidc.java.server.datastored.user.claims;

import app.cbo.oidc.java.server.datastored.user.UserId;

/**
 * Address claim take the form of "address": [JSON OBJECT]
 * <p>
 * Th
 */
public record Address(UserId userId, AddressPayload address) implements ScopedClaims {

    public Address(UserId userId, String formatted, String street_address, String locality, String region,
                   String postal_code, String country) {

        this(userId, new AddressPayload(formatted, street_address, locality, region, postal_code, country));
    }

    public static record AddressPayload(String formatted, String street_address, String locality, String region,
                                        String postal_code, String country) {

    }
}
