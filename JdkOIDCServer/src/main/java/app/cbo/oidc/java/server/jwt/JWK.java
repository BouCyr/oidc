package app.cbo.oidc.java.server.jwt;

import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

public record JWK(String kty, String use, String alg, String kid, String n, String e) {
    /*
    https://www.rfc-editor.org/rfc/rfc7517#page-5
    {"kty":"EC",
      "crv":"P-256",
      "x":"f83OJ3D2xF1Bg8vub9tLe1gHMzV76e8Tus9uPHvRVEU",
      "y":"x_FEzRu9m36HLN_tue659LNpXW6pCyStikYjKIWI5a0",
      "kid":"Public key used in JWS spec Appendix A.3 example"
     }
     */
    //cf https://www.rfc-editor.org/rfc/rfc7518#section-6.3


    public static JWK rsaPublicKey(String kid, RSAPublicKey publicKey) {
        //[24/04/2023] I read:
        //https://datatracker.ietf.org/doc/html/rfc7518#page-30
        // > The "n" (modulus) parameter contains the modulus value for the RSA public key.  It is represented as a Base64urlUInt-encoded value.
        // > The "e" (exponent) parameter contains the exponent value for the RSA public key.  It is represented as a Base64urlUInt-encoded value.
        //
        // My first impl 'Base64.getUrlEncoder().encodeToString(publicKey.getModulus().toByteArray())' somehow works straight away...


        return new JWK(JWA.RS256.type(), "sig", JWA.RS256.rfcName(), kid,
                Base64.getUrlEncoder().encodeToString(publicKey.getModulus().toByteArray()),
                Base64.getUrlEncoder().encodeToString(publicKey.getPublicExponent().toByteArray()));
    }
}
