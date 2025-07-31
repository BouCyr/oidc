package app.cbo.oidc.java.server.webauthn.core;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents the authenticator's response for a registration ceremony.
 *
 * @param clientDataJSON The JSON-compatible client data produced by the client.
 * @param attestationObject The attestation object produced by the authenticator.
 */
public record AuthenticatorAttestationResponse(
    byte[] clientDataJSON,
    byte[] attestationObject
) {
    // Canonical constructor with null checks
    public AuthenticatorAttestationResponse(byte[] clientDataJSON, byte[] attestationObject) {
        this.clientDataJSON = Objects.requireNonNull(clientDataJSON, "clientDataJSON must not be null");
        this.attestationObject = Objects.requireNonNull(attestationObject, "attestationObject must not be null");
    }

    // Override equals and hashCode for byte[] fields
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticatorAttestationResponse that = (AuthenticatorAttestationResponse) o;
        return Arrays.equals(clientDataJSON, that.clientDataJSON) &&
               Arrays.equals(attestationObject, that.attestationObject);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(clientDataJSON);
        result = 31 * result + Arrays.hashCode(attestationObject);
        return result;
    }

    // Override toString for better readability of byte[] fields
    @Override
    public String toString() {
        return "AuthenticatorAttestationResponse{" +
               "clientDataJSON_length=" + (clientDataJSON != null ? clientDataJSON.length : "null") + // Or Base64 for short ones
               ", attestationObject_length=" + (attestationObject != null ? attestationObject.length : "null") + // Or Base64
               '}';
    }
}
