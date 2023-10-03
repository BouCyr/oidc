package app.cbo.oidc.java.server.http.userinfo;

@FunctionalInterface
public interface AccessTokenValidator {

    AccessTokenData validateAccessToken(String accessToken) throws ForbiddenResponse;
}
