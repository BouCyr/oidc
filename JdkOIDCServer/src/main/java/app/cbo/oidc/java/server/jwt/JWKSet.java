package app.cbo.oidc.java.server.jwt;

import java.util.List;

/**
 * See RFC#5717 par. 5
 * <p>
 * >>    A JWK Set is a JSON object that represents a set of JWKs.  The JSON
 * >>    object MUST have a "keys" member, with its value being an array of
 * >>    JWKs.  This JSON object MAY contain whitespace and/or line breaks.
 * >>
 * >>    The member names within a JWK Set MUST be unique; JWK Set parsers
 * >>    MUST either reject JWK Sets with duplicate member names or use a JSON
 * >>    parser that returns only the lexically last duplicate member name, as
 * >>    specified in Section 15.12 ("The JSON Object") of ECMAScript 5.1
 * >>    [ECMAScript].
 * >>
 * >>    Additional members can be present in the JWK Set; if not understood
 * >>    by implementations encountering them, they MUST be ignored.
 * >>    Parameters for representing additional properties of JWK Sets should
 * >>    either be registered in the IANA "JSON Web Key Set Parameters"
 * >>    registry established by Section 8.4 or be a value that contains a
 * >>    Collision-Resistant Name.
 * >>
 * >>    Implementations SHOULD ignore JWKs within a JWK Set that use "kty"
 * >>    (key type) values that are not understood by them, that are missing
 * >>    required members, or for which values are out of the supported
 * >>    ranges.
 */
public record JWKSet(List<JWK> keys) {

}
