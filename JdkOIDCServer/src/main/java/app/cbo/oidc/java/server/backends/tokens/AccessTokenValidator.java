package app.cbo.oidc.java.server.backends.tokens;

import app.cbo.oidc.java.server.http.userinfo.ForbiddenResponse;

@FunctionalInterface
public interface AccessTokenValidator {

    AccessTokenData validateAccessToken(String accessToken) throws ForbiddenResponse;
}
