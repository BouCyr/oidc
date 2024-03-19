package app.cbo.oidc.java.server.backends.claims;

import app.cbo.oidc.java.server.datastored.user.UserId;

import java.util.Map;
import java.util.Set;

@FunctionalInterface
public interface ClaimsResolver {


    /**
     * For a list of requested scope, returns the user info in the form of Map (with field name as string, and the value as a String or Number, except for Address)
     *
     * @param userId          the user being requested/authenticated
     * @param requestedScopes the list of requested scopes (for id_token/userinfo contents)
     * @return a Map, pretty much ready for JSON serialization
     */
    Map<String, Object> claimsFor(UserId userId, Set<String> requestedScopes);
}
