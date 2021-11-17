package app.cbo.oidc.toyidc.endpoints;

import java.util.*;
import java.util.stream.Stream;

public enum Flow {
// #3.1.2.1 When using the Authorization Code Flow, this value is 'code'.
    AuthorizationCode("code"),
// #3.2.2.1 When using the Implicit Flow, this value is 'id_token token' or 'id_token'.
    Implicit("id_token"),
    ImplicaitWithAccessToken("id_token token"),
// When using the Hybrid Flow, this value is 'code id_token', 'code token', or 'code id_token token'.
// The meanings of these values are defined in OAuth 2.0 Multiple Response Type Encoding Practices [OAuth.Responses].
    HybridWithId("code id_token"),
    HybridWithAccessToken("code token"),
    HybridWithBoth("code id_token token");

    private final Set<String> responseTypes;

    Flow(String... responseTypes) {
        this.responseTypes = Set.of(responseTypes);

    }

    public static Optional<Flow> fromResponseType(String rs){
        return Stream.of(Flow.values())
                .filter(f -> f.responseTypes.contains(rs))
                .findFirst();
    }
}
