package app.cbo.oidc.java.server.webauthn.core;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Options for retrieving an existing public key credential.
 *
 * @param challenge A challenge to prevent replay attacks (byte array).
 * @param timeout The time, in milliseconds, that the Relying Party is willing to wait for the call to complete (optional).
 * @param rpId The Relying Party identifier (e.g., domain string) (optional).
 * @param allowCredentials A list of credentials acceptable to the Relying Party (optional).
 * @param userVerification The Relying Party's requirements for user verification (e.g., "required", "preferred", "discouraged") (optional).
 * @param extensions Additional parameters for the client and authenticator (optional).
 */
public record PublicKeyCredentialRequestOptions(
    byte[] challenge,
    Optional<Long> timeout,
    Optional<String> rpId,
    Optional<List<PublicKeyCredentialDescriptor>> allowCredentials,
    Optional<String> userVerification,
    Optional<Map<String, Object>> extensions
) {
    // Canonical constructor with explicit Optional handling and null checks for mandatory fields
    public PublicKeyCredentialRequestOptions(
            byte[] challenge,
            Optional<Long> timeout,
            Optional<String> rpId,
            Optional<List<PublicKeyCredentialDescriptor>> allowCredentials,
            Optional<String> userVerification,
            Optional<Map<String, Object>> extensions) {
        this.challenge = Objects.requireNonNull(challenge, "challenge must not be null");
        this.timeout = timeout == null ? Optional.empty() : timeout;
        this.rpId = rpId == null ? Optional.empty() : rpId;
        this.allowCredentials = allowCredentials == null ? Optional.empty() : allowCredentials;
        this.userVerification = userVerification == null ? Optional.empty() : userVerification;
        this.extensions = extensions == null ? Optional.empty() : extensions;
    }

    // Convenience constructor for mandatory fields
    public PublicKeyCredentialRequestOptions(byte[] challenge) {
        this(challenge, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    // Override equals and hashCode for byte[] challenge field
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicKeyCredentialRequestOptions that = (PublicKeyCredentialRequestOptions) o;
        return Arrays.equals(challenge, that.challenge) &&
               Objects.equals(timeout, that.timeout) &&
               Objects.equals(rpId, that.rpId) &&
               Objects.equals(allowCredentials, that.allowCredentials) &&
               Objects.equals(userVerification, that.userVerification) &&
               Objects.equals(extensions, that.extensions);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(timeout, rpId, allowCredentials, userVerification, extensions);
        result = 31 * result + Arrays.hashCode(challenge);
        return result;
    }

    // Override toString for better readability of byte[] challenge
    @Override
    public String toString() {
        return "PublicKeyCredentialRequestOptions{" +
               "challenge=" + Arrays.toString(challenge) + // Or Base64 representation
               ", timeout=" + timeout +
               ", rpId=" + rpId +
               ", allowCredentials=" + allowCredentials +
               ", userVerification=" + userVerification +
               ", extensions=" + extensions +
               '}';
    }
}
