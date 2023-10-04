package app.cbo.oidc.java.server.http.token;

        /*
        After receiving and validating a valid and authorized Token Request from the Client, the Authorization Server returns a successful response that includes an ID Token and an Access Token.
        The parameters in the successful response are defined in Section 4.1.4 of OAuth 2.0 [RFC6749]. The response uses the application/json media type.
        The OAuth 2.0 token_type response parameter value MUST be Bearer, as specified in OAuth 2.0 Bearer Token Usage [RFC6750], unless another Token Type has been negotiated with the Client.
        Servers SHOULD support the Bearer Token Type; use of other Token Types is outside the scope of this specification.

          In addition to the response parameters specified by OAuth 2.0, the following parameters MUST be included in the response:

        id_token
        ID Token value associated with the authenticated session.
        All Token Responses that contain tokens, secrets, or other sensitive information MUST include the following HTTP response header fields and values:


        Header Name	Header Value
        Cache-Control	no-store
        Pragma	no-cache
        */

import java.time.Duration;
import java.util.Collection;

/* OAuth2.0 4.1.4
{
    "access_token":"2YotnFZFEjr1zCsicMWpAA",
    "token_type":"example",
    "expires_in":3600,
    "refresh_token":"tGzv3JOkF0XG5Qx2TlKWIA",
    "example_parameter":"example_value"
     }
*/
public record TokenResponse(String access_token,
                            String token_type,
                            long expires_in,
                            String refresh_token,
                            String id_token,
                            String scope) {


    public TokenResponse(String access_token, String refresh_token, String id_token, Duration ttl, Collection<String> scopes) {
        this(access_token,
                "Bearer",
                ttl.toSeconds(),
                refresh_token,
                id_token,
                String.join(" ", scopes));

    }


}
