package app.cbo.oidc.java.server.http.token;


import java.time.Duration;
import java.util.Collection;

/**
 * The authorization server issues an access token and optional refresh
 * token, and constructs the response by adding the following parameters
 * to the entity-body of the HTTP response with a 200 (OK) status code:
 *
 * <ul>
 *    <li>access_token: <b>REQUIRED</b>.  The access token issued by the authorization server.</li>
 *    <li>token_type: <b>REQUIRED.</b>  The type of the token issued as described in
 *          Section 7.1.  Value is case insensitive.</li>
 *
 *    <li>expires_in: <b>RECOMMENDED</b>.  The lifetime in seconds of the access token.  For
 *          example, the value "3600" denotes that the access token will
 *          expire in one hour from the time the response was generated.
 *          If omitted, the authorization server SHOULD provide the
 *          expiration time via other means or document the default value.</li>
 *    <li>refresh_token : <b>OPTIONAL</b>.  The refresh token, which can be used to obtain new
 *          access tokens using the same authorization grant as described
 *          in Section 6.</li>
 *
 *    <li>scope : <b>OPTIONAL</b>, if identical to the scope requested by the client;
 *          otherwise, REQUIRED.  The scope of the access token as
 *          described by Section 3.3.
 * </ul>
 */
public record RefreshResponse(String access_token,
                              String token_type,
                              long expires_in,
                              String scope) {

    public RefreshResponse(String access_token, Duration ttl, Collection<String> scopes) {
        this(access_token,
                "Bearer",
                ttl.toSeconds(),
                String.join(" ", scopes));

    }
}
