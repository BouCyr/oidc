package app.cbo.oidc.java.server.backends.claims;

import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.datastored.user.claims.ScopedClaims;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Stores the claims/user find/user data of every registered users
 */
public class MemClaims implements Claims {

    private final static Logger LOGGER = Logger.getLogger(MemClaims.class.getCanonicalName());

    List<ScopedClaims> allClaims = new ArrayList<>();

    public MemClaims() {
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void store(ScopedClaims... someClaims) {
        Stream.of(someClaims)
                .forEach(scopedClaims -> {
                    if (allClaims.removeIf(sc -> sc.userId().equals(scopedClaims.userId()) && sc.getClass().equals(scopedClaims.getClass()))) {
                        LOGGER.info(scopedClaims.userId().getUserId() + " already had a stored '" + scopedClaims.scopeName() + "' scope. Replacing");
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
                .filter(claim -> Claims.isInScope(requestedScopes, claim))
                .forEach(claims -> Claims.toNameAndValue(claims).forEach(pair -> result.put(pair.left(), pair.right())));

        result.remove("userId");

        return result;
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


}
