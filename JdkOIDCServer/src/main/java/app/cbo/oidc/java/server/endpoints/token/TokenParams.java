package app.cbo.oidc.java.server.endpoints.token;

import app.cbo.oidc.java.server.jsr305.NotNull;

import java.util.Collection;
import java.util.Map;

import static app.cbo.oidc.java.server.utils.ParamsHelper.singleParam;

//3.1.3.1.  Token Request
public record TokenParams(String grantType, String code, String redirectUri, String clientId) {
    //rfc6749 Oauth2 #section-4.1.3
    // grant_type REQUIRED.  Value MUST be set to "authorization_code".
    public final static String GRANT_TYPE = "grant_type";

    //code REQUIRED.  The authorization code received from the authorization server.
    public final static String CODE = "code";

    // redirect_uri REQUIRED, if the "redirect_uri" parameter was included in the authorization request as described in Section 4.1.1, and their values MUST be identical.
    public final static String REDIRECT_URI = "redirect_uri";

    //client_id REQUIRED, if the client is not authenticating with the authorization server as described in Section 3.2.1.
    public final static String CLIENT_ID = "client_id";

    public TokenParams(@NotNull Map<String, Collection<String>> params) {
        this(
                singleParam(params.get(GRANT_TYPE)).orElse(null),
                singleParam(params.get(CODE)).orElse(null),
                singleParam(params.get(REDIRECT_URI)).orElse(null),
                singleParam(params.get(CLIENT_ID)).orElse(null));
    }
}
