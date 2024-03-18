package app.cbo.oidc.java.server.backends.keys;

import app.cbo.oidc.java.server.datastored.KeyId;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jwt.JWKSet;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;

/**
 * This interface represents a set of keys used for cryptographic operations.
 * It provides methods to get the current key, rotate the keys, and get the private and public keys.
 * The keys are identified by a KeyId.
 */
public interface KeySet {

    /**
     * This method returns the current set of keys as a JWKSet.
     *
     * @return the current set of keys as a JWKSet.
     */
    @NotNull
    JWKSet asJWKSet();

    /**
     * This method returns the KeyId of the current key.
     *
     * @return the KeyId of the current key.
     */
    @NotNull
    KeyId current();

    /**
     * This method returns the private key associated with the provided KeyId.
     * If no key is found for the provided KeyId, it returns an empty Optional.
     *
     * @param keyId the KeyId of the key to retrieve.
     * @return the private key associated with the provided KeyId, or an empty Optional if no key is found.
     */
    @NotNull
    Optional<PrivateKey> privateKey(@NotNull KeyId keyId);

    /**
     * This method returns the public key associated with the provided KeyId.
     * If no key is found for the provided KeyId, it returns an empty Optional.
     *
     * @param keyId the KeyId of the key to retrieve.
     * @return the public key associated with the provided KeyId, or an empty Optional if no key is found.
     */
    @NotNull
    Optional<PublicKey> publicKey(@NotNull KeyId keyId);

    /**
     * This method rotates the keys and returns the KeyId of the new current key.
     *
     * @return the KeyId of the new current key.
     */
    @NotNull
    KeyId rotate();
}