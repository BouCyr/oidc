package app.cbo.oidc.java.server.endpoints.token;

import app.cbo.oidc.java.server.backends.Codes;
import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Code;
import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;

import java.util.logging.Logger;

public class TokenEndpoint {

    private final static Logger LOGGER = Logger.getLogger(TokenEndpoint.class.getCanonicalName());

    private static TokenEndpoint instance = null;

    private TokenEndpoint() {
    }

    public static TokenEndpoint getInstance() {
        if (instance == null) {
            instance = new TokenEndpoint();
        }
        return instance;
    }

    @NotNull
    public Interaction treatRequest(@NotNull TokenParams params) {
        /*
        The Authorization Server MUST validate the Token Request as follows:

        Authenticate the Client if it was issued Client Credentials or if it uses another Client Authentication method, per Section 9.
        Ensure the Authorization Code was issued to the authenticated Client.
        Verify that the Authorization Code is valid.
                If possible, verify that the Authorization Code has not been previously used.
                Ensure that the redirect_uri parameter value is identical to the redirect_uri parameter value that was included in the initial Authorization Request. If the redirect_uri parameter value is not present when there is only one registered redirect_uri value, the Authorization Server MAY return an error (since the Client should have included the parameter) or MAY proceed without an error (since OAuth 2.0 permits the parameter to be omitted in this case).
        Verify that the Authorization Code used was issued in response to an OpenID Connect Authentication Request (so that an ID Token will be returned from the Token Endpoint).
        */
        if (!this.validateParams(params/*, credentials*/)) {
            return new JsonError(HttpCode.BAD_REQUEST, "client_id forbidden");
        }


        if (!this.validateClientId(params/*, credentials*/)) {
            return new JsonError(HttpCode.BAD_REQUEST, "client_id forbidden");
        }

        Codes.getInstance().consume(Code.of(params.code()), ClientId.of(params.clientId()), params.redirectUri());




        /*
        After receiving and validating a valid and authorized Token Request from the Client, the Authorization Server returns a successful response that includes an ID Token and an Access Token. The parameters in the successful response are defined in Section 4.1.4 of OAuth 2.0 [RFC6749]. The response uses the application/json media type.

                The OAuth 2.0 token_type response parameter value MUST be Bearer, as specified in OAuth 2.0 Bearer Token Usage [RFC6750], unless another Token Type has been negotiated with the Client. Servers SHOULD support the Bearer Token Type; use of other Token Types is outside the scope of this specification.

                In addition to the response parameters specified by OAuth 2.0, the following parameters MUST be included in the response:

        id_token
        ID Token value associated with the authenticated session.
        All Token Responses that contain tokens, secrets, or other sensitive information MUST include the following HTTP response header fields and values:


        Header Name	Header Value
        Cache-Control	no-store
        Pragma	no-cache

        HTTP Response Headers and Values*/
        return new JsonError(HttpCode.BAD_REQUEST, "TODO");

    }

    private boolean validateParams(TokenParams params) {
        //TODO [06/04/2023]
        return true;
    }

    private boolean validateClientId(TokenParams params) {
        //TODO [06/04/2023]
        return true;
    }


}
