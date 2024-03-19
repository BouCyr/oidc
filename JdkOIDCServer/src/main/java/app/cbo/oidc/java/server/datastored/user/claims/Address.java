package app.cbo.oidc.java.server.datastored.user.claims;

import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.json.JSON;

/**
 * Address claim take the form of "address": [JSON OBJECT]
 * <p>
 */
public record Address(UserId userId, String address) implements ScopedClaims {

    public Address(UserId userId, String formatted, String street_address, String locality, String region,
                   String postal_code, String country) {

        this(userId, new AddressPayload(formatted, street_address, locality, region, postal_code, country));
    }

    public Address(UserId userId, AddressPayload addressPayload) {
        this(userId, JSON.jsonifyOneline(addressPayload));
    }

    @Override
    public String scopeName() {
        return "address";
    }

    public record AddressPayload(String formatted,
                                 String street_address,
                                 String locality,
                                 String region,
                                 String postal_code,
                                 String country) {

    }
}
