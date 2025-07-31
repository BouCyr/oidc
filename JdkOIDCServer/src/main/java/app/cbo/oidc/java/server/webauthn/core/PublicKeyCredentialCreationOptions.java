package app.cbo.oidc.java.server.webauthn.core;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Options for creating a new public key credential.
 *
 * @param rp The Relying Party entity.
 * @param user The User entity.
 * @param challenge A challenge to prevent replay attacks (byte array).
 * @param pubKeyCredParams A list of public key credential parameters supported by the Relying Party.
 * @param timeout The time, in milliseconds, that the Relying Party is willing to wait for the call to complete (optional).
 * @param excludeCredentials A list of credentials to exclude from creation (optional).
 * @param authenticatorSelection Criteria for authenticator selection (optional).
 * @param attestation The Relying Party's preference for attestation conveyance (e.g., "none", "indirect", "direct") (optional).
 * @param extensions Additional parameters for the client and authenticator (optional).
 */
public record PublicKeyCredentialCreationOptions(
    RpEntity rp,
    UserEntity user,
    byte[] challenge,
    List<PublicKeyCredentialParameter> pubKeyCredParams,
    Optional<Long> timeout,
    Optional<List<PublicKeyCredentialDescriptor>> excludeCredentials,
    Optional<AuthenticatorSelectionCriteria> authenticatorSelection,
    Optional<String> attestation,
    Optional<Map<String, Object>> extensions
) {
    // Canonical constructor with explicit Optional handling
    public PublicKeyCredentialCreationOptions(
            RpEntity rp,
            UserEntity user,
            byte[] challenge,
            List<PublicKeyCredentialParameter> pubKeyCredParams,
            Optional<Long> timeout,
            Optional<List<PublicKeyCredentialDescriptor>> excludeCredentials,
            Optional<AuthenticatorSelectionCriteria> authenticatorSelection,
            Optional<String> attestation,
            Optional<Map<String, Object>> extensions) {
        this.rp = Objects.requireNonNull(rp, "rp must not be null");
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.challenge = Objects.requireNonNull(challenge, "challenge must not be null");
        this.pubKeyCredParams = Objects.requireNonNull(pubKeyCredParams, "pubKeyCredParams must not be null");
        this.timeout = timeout == null ? Optional.empty() : timeout;
        this.excludeCredentials = excludeCredentials == null ? Optional.empty() : excludeCredentials;
        this.authenticatorSelection = authenticatorSelection == null ? Optional.empty() : authenticatorSelection;
        this.attestation = attestation == null ? Optional.empty() : attestation;
        this.extensions = extensions == null ? Optional.empty() : extensions;
    }

    // Convenience constructor for mandatory fields + common optionals
    public PublicKeyCredentialCreationOptions(
            RpEntity rp,
            UserEntity user,
            byte[] challenge,
            List<PublicKeyCredentialParameter> pubKeyCredParams) {
        this(rp, user, challenge, pubKeyCredParams, Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }


    // Override equals and hashCode for byte[] challenge field
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicKeyCredentialCreationOptions that = (PublicKeyCredentialCreationOptions) o;
        return Objects.equals(rp, that.rp) &&
               Objects.equals(user, that.user) &&
               Arrays.equals(challenge, that.challenge) &&
               Objects.equals(pubKeyCredParams, that.pubKeyCredParams) &&
               Objects.equals(timeout, that.timeout) &&
               Objects.equals(excludeCredentials, that.excludeCredentials) &&
               Objects.equals(authenticatorSelection, that.authenticatorSelection) &&
               Objects.equals(attestation, that.attestation) &&
               Objects.equals(extensions, that.extensions);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(rp, user, pubKeyCredParams, timeout, excludeCredentials,
                                  authenticatorSelection, attestation, extensions);
        result = 31 * result + Arrays.hashCode(challenge);
        return result;
    }

    // Override toString for better readability of byte[] challenge
    @Override
    public String toString() {
        return "PublicKeyCredentialCreationOptions{" +
               "rp=" + rp +
               ", user=" + user +
               ", challenge=" + Arrays.toString(challenge) + // Or Base64 representation
               ", pubKeyCredParams=" + pubKeyCredParams +
               ", timeout=" + timeout +
               ", excludeCredentials=" + excludeCredentials +
               ", authenticatorSelection=" + authenticatorSelection +
               ", attestation=" + attestation +
               ", extensions=" + extensions +
               '}';
    }
}
