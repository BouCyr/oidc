package app.cbo.oidc.java.server.backends.claims;

import app.cbo.oidc.java.server.datastored.user.claims.Address;
import app.cbo.oidc.java.server.datastored.user.claims.Mail;
import app.cbo.oidc.java.server.datastored.user.claims.Phone;
import app.cbo.oidc.java.server.datastored.user.claims.Profile;
import app.cbo.oidc.java.server.datastored.user.claims.ScopedClaims;
import app.cbo.oidc.java.server.utils.Pair;
import app.cbo.oidc.java.server.utils.ReflectionUtils;

import java.util.Collection;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This interface provides methods for resolving and storing claims.
 * It also provides utility methods for working with ScopedClaims.
 */
public interface Claims extends ClaimsResolver, ClaimsStorer {

    Logger LOGGER = Logger.getLogger(Claims.class.getCanonicalName());

    /**
     * Checks if a specificic ScopedClaim is requested
     *
     * @param scopes       the list of scopes requested
     * @param scopedClaims the scope being checked
     * @return true if @param scopedClaims is requested
     */
    static boolean isInScope(Set<String> scopes, ScopedClaims scopedClaims) {
        return (
                (scopes.contains("profile") && scopedClaims instanceof Profile)
                        || (scopes.contains("phone") && scopedClaims instanceof Phone)
                        || (scopes.contains("email") && scopedClaims instanceof Mail)
                        || (scopes.contains("address") && scopedClaims instanceof Address)
        );
    }

    /**
     * Transforms a ScopedClaims into a Collection of key/value pair, vith the field name as key and the user info in value
     *
     * @param scopedClaims the user ScopedClaims being transformed
     * @return a Collection of key/value pair, vith the field name as key and the user info in value
     */
    static Collection<Pair<String, Object>> toNameAndValue(ScopedClaims scopedClaims) {
        return ReflectionUtils.toNameAndValue(scopedClaims)
                .stream()
                .map(Claims::toPair) // call the supplier, trap any exception
                .filter(pair -> pair.right() != null)//5.3.2 -> If a Claim is not returned, that Claim Name SHOULD be omitted from the JSON object representing the Claims; it SHOULD NOT be present with a null or empty string value
                .toList();

    }

    /**
     * Transform a NameAndValue into a pait<String, Object></String,>
     *
     * @param nv being transforms
     * @return a Pair, with the name as Left and the value as Right
     */
    static Pair<String, Object> toPair(ReflectionUtils.NameAndValue nv) {
        var name = nv.name();
        Object value;
        try {
            value = nv.getValue().get();
        } catch (Exception e) {
            LOGGER.info(STR."error while retrieving value of \{name}. Assuming null");
            value = null;
        }
        return new Pair<>(name, value);
    }
}
