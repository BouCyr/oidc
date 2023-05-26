package app.cbo.oidc.java.server.endpoints.authorize;

import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;
import app.cbo.oidc.java.server.utils.HttpCode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.time.Duration;

public record ImplicitFlowSuccessInteraction(@NotNull AuthorizeParams params, @NotNull String idTokenJWS,
                                             @Nullable String accessToken,
                                             @Nullable Duration accessTokenTTL) implements Interaction {


    public static ImplicitFlowSuccessInteraction withAccessToken(AuthorizeParams params, String idToken, String accessToken, Duration accessTokenTTL) {
        return new ImplicitFlowSuccessInteraction(params, idToken, accessToken, accessTokenTTL);
    }

    public static ImplicitFlowSuccessInteraction withoutAccessToken(AuthorizeParams params, String idToken) {
        return new ImplicitFlowSuccessInteraction(params, idToken, null, null);
    }


    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {

        /*
        3.2.2.5.  Successful Authentication Response:

"When using the Implicit Flow, all response parameters are added to the fragment component of the Redirection URI,
 as specified in OAuth 2.0 Multiple Response Type Encoding Practices [OAuth.Responses], unless a different Response Mode was specified."

        These parameters are returned from the Authorization Endpoint:

access_token : OAuth 2.0 Access Token.
This is returned unless the response_type value used is id_token.

expires_in : OPTIONAL.
Expiration time of the Access Token in seconds since the response was generated.

token_type : OAuth 2.0 Token Type value.
The value MUST be Bearer or another token_type value that the Client has negotiated with the Authorization Server.
Clients implementing this profile MUST support the OAuth 2.0 Bearer Token Usage [RFC6750] specification.
This profile only describes the use of bearer tokens. This is returned in the same cases as access_token is.

id_token : REQUIRED. ID Token.

state : OAuth 2.0 state value.
REQUIRED if the state parameter is present in the Authorization Request. Clients MUST verify that the state value is equal to the value of state parameter in the Authorization Request.


         */

        if (params.redirectUri().isEmpty()) {
            //check should have been done long before reaching this point
            throw new IllegalArgumentException("redirect_uri empty");
        }

        String uri = params.redirectUri().get() + "#id_token=" + idTokenJWS();

        if (params.state().isPresent()) {
            uri += "&state=" + params.state().get();
        }

        if (accessToken() != null) {
            uri += "access_token=" + accessToken() + "&token_type=bearer";
            if (accessTokenTTL() != null) {
                uri += "&expires_in=" + accessTokenTTL().toSeconds();
            }
        }
        exchange.getResponseHeaders().add("Location", uri);
        exchange.sendResponseHeaders(HttpCode.FOUND.code(), 0);
        exchange.getResponseBody().flush();
        exchange.getResponseBody().close();
        return;

    }
}
