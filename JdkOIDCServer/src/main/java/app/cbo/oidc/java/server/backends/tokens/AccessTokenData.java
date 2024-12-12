package app.cbo.oidc.java.server.backends.tokens;

import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;
import app.cbo.oidc.java.server.oidc.Issuer;

import java.util.Set;
// RFC 7662 Token introspection 2.2

/**

 */
public record AccessTokenData(
        @NotNull boolean active,
        @Nullable Set<String> scopes,
        @Nullable ClientId clientId,
        @Nullable UserId sub,
        @Nullable String tokenType,
        @Nullable Long exp,
        @Nullable Long iat,
        @Nullable Long nbf,
        @Nullable String aud,
        @Nullable Issuer iss,
        @Nullable String jti
) {
}
