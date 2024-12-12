package app.cbo.oidc.java.server.http.token.params;

import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Code;

public interface CodeToTokenParams {

    //rfc6749 Oauth2 #section-4.1.3
    // grant_type REQUIRED.  Value MUST be set to "authorization_code".
    String grantType();

    //code REQUIRED.  The authorization code received from the authorization server.
    Code code();

    // redirect_uri REQUIRED, if the "redirect_uri" parameter was included in the authorization request as described in Section 4.1.1, and their values MUST be identical.
    String redirectUri();

    //client_id REQUIRED, if the client is not authenticating with the authorization server as described in Section 3.2.1.
    ClientId clientId();

}
