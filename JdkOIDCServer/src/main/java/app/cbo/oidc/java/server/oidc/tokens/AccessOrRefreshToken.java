package app.cbo.oidc.java.server.oidc.tokens;

import java.util.Collection;

public record AccessOrRefreshToken(String sub, long exp, Collection<String> scopes) {
}
