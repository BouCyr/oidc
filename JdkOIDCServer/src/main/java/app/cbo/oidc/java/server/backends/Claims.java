package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.datastored.user.claims.*;
import app.cbo.oidc.java.server.utils.Pair;
import app.cbo.oidc.java.server.utils.ReflectionUtils;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class Claims {

    private final static Logger LOGGER = Logger.getLogger(Claims.class.getCanonicalName());

    private static Claims instance = null;
    List<ScopedClaims> allClaims = new ArrayList<>();

    private Claims() {
    }

    public static Claims getInstance() {
        if (instance == null) {
            instance = new Claims();
        }
        return instance;
    }

    public void store(ScopedClaims... someClaims) {
        Stream.of(someClaims)
                .forEach(scopedClaims -> {
                    if (allClaims.removeIf(sc -> sc.userId().equals(scopedClaims.userId()) && sc.getClass().equals(scopedClaims.getClass()))) {
                        LOGGER.info(scopedClaims.userId().getUserId() + " already had a " + scopedClaims.getClass().getSimpleName() + ". Replacing");
                    }

                    allClaims.add(scopedClaims);
                });
    }

    public Map<String, Object> claimsFor(UserId userId, Set<String> requestedScopes) {

        final Map<String, Object> result = new HashMap<>();
        this.filterByUser(userId)
                .stream()
                .filter(claims -> this.isInScope(requestedScopes, claims))
                .forEach(claims -> toNameAndValue(claims).forEach(pair -> result.put(pair.left(), pair.right())));

        result.remove("userId");

        return result;
    }

    private boolean isInScope(Set<String> scopes, ScopedClaims scopedClaims) {
        return (
                (scopes.contains("profile") && scopedClaims instanceof Profile)
                        || (scopes.contains("phone") && scopedClaims instanceof Phone)
                        || (scopes.contains("email") && scopedClaims instanceof Mail)
                        || (scopes.contains("address") && scopedClaims instanceof Address)
        );
    }

    List<ScopedClaims> filterByUser(UserId userId) {

        return this.allClaims
                .stream()
                .filter(claims -> claims.userId().equals(userId))
                .toList();

    }

    Collection<Pair<String, Object>> toNameAndValue(ScopedClaims scopedClaims) {
        return ReflectionUtils.toNameAndValue(scopedClaims)
                .stream()
                .map(this::toPair) // call the supplier, trap any exception
                .filter(pair -> pair.right() != null)//5.3.2 -> If a Claim is not returned, that Claim Name SHOULD be omitted from the JSON object representing the Claims; it SHOULD NOT be present with a null or empty string value
                .toList();

    }

    private Pair<String, Object> toPair(ReflectionUtils.NameAndValue nv) {
        var name = nv.name();
        Object value;
        try {
            value = nv.getValue().get();
        } catch (Exception e) {
            LOGGER.info("error while retrieving value of " + name + ". Assuming null");
            value = null;
        }
        return new Pair<>(name, value);
    }

}
