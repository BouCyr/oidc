package app.cbo.oidc.java.server.backends.tokens;

import app.cbo.oidc.java.server.datastored.user.UserId;

import java.util.Set;

public record AccessTokenData(UserId sub, String aud, Set<String> scopes) {
}
