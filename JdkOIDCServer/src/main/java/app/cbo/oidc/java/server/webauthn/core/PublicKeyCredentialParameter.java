package app.cbo.oidc.java.server.webauthn.core;

/**
 * Describes a public key credential type supported by the Relying Party.
 * @param type The type of credential (e.g., "public-key").
 * @param alg The COSE algorithm identifier for the cryptographic signature algorithm.
 */
public record PublicKeyCredentialParameter(
    String type,
    int alg
) {
}
