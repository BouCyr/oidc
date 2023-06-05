package app.cbo.oidc.java.server.backends.claims;

import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.datastored.user.claims.*;
import app.cbo.oidc.java.server.utils.Pair;
import app.cbo.oidc.java.server.utils.ReflectionUtils;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Stores the claims/user fino/user data of every registered users
 */
public class Claims implements ClaimsStorer, ClaimsResolver {

    private final static Logger LOGGER = Logger.getLogger(Claims.class.getCanonicalName());

    List<ScopedClaims> allClaims = new ArrayList<>();

    public Claims() {
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void store(ScopedClaims... someClaims) {
        Stream.of(someClaims)
                .forEach(scopedClaims -> {
                    if (allClaims.removeIf(sc -> sc.userId().equals(scopedClaims.userId()) && sc.getClass().equals(scopedClaims.getClass()))) {
                        LOGGER.info(scopedClaims.userId().getUserId() + " already had a " + scopedClaims.getClass().getSimpleName() + ". Replacing");
                    }

                    allClaims.add(scopedClaims);
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> claimsFor(UserId userId, Set<String> requestedScopes) {

        final Map<String, Object> result = new HashMap<>();
        this.filterByUser(userId)
                .stream()
                .filter(claim -> this.isInScope(requestedScopes, claim))
                .forEach(claims -> toNameAndValue(claims).forEach(pair -> result.put(pair.left(), pair.right())));

        result.remove("userId");

        return result;
    }

    /**
     * Checks if a specificic ScopedClaim is requested
     *
     * @param scopes       the list of scopes requested
     * @param scopedClaims the scope being checked
     * @return true if @param scopedClaims is requested
     */
    private boolean isInScope(Set<String> scopes, ScopedClaims scopedClaims) {
        return (
                (scopes.contains("profile") && scopedClaims instanceof Profile)
                        || (scopes.contains("phone") && scopedClaims instanceof Phone)
                        || (scopes.contains("email") && scopedClaims instanceof Mail)
                        || (scopes.contains("address") && scopedClaims instanceof Address)
        );
    }

    /**
     * Returns all known info for a user
     *
     * @param userId the userId
     * @return every ScopedClaims of this user
     */
    List<ScopedClaims> filterByUser(UserId userId) {

        return this.allClaims
                .stream()
                .filter(claims -> claims.userId().equals(userId))
                .toList();

    }

    /**
     * TRansfomr a ScopedClaims into a Collection of key/value pair, vith the field name as key and the user info in value
     *
     * @param scopedClaims the user ScopedClaims being trnasformed
     * @return a Collection of key/value pair, vith the field name as key and the user info in value
     */
    Collection<Pair<String, Object>> toNameAndValue(ScopedClaims scopedClaims) {
        return ReflectionUtils.toNameAndValue(scopedClaims)
                .stream()
                .map(this::toPair) // call the supplier, trap any exception
                .filter(pair -> pair.right() != null)//5.3.2 -> If a Claim is not returned, that Claim Name SHOULD be omitted from the JSON object representing the Claims; it SHOULD NOT be present with a null or empty string value
                .toList();

    }

    /**
     * Transform a NameAndValue into a pait<String, Object></String,>
     *
     * @param nv being transforms
     * @return a Pair, with the name as Left and the value as Right
     */
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
