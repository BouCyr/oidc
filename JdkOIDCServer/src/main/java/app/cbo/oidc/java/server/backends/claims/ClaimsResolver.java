package app.cbo.oidc.java.server.backends.claims;

import app.cbo.oidc.java.server.datastored.user.UserId;

import java.util.Map;
import java.util.Set;

@FunctionalInterface
public interface ClaimsResolver {
    Map<String, Object> claimsFor(UserId userId, Set<String> requestedScopes);
}
