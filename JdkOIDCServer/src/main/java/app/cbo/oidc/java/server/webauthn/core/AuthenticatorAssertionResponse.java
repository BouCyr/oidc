package app.cbo.oidc.java.server.webauthn.core;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the authenticator's response for an authentication ceremony.
 *
 * @param clientDataJSON The JSON-compatible client data produced by the client.
 * @param authenticatorData The authenticator data produced by the authenticator.
 * @param signature The signature produced by the authenticator.
 * @param userHandle The user handle produced by the authenticator (optional).
 */
public record AuthenticatorAssertionResponse(
    byte[] clientDataJSON,
    byte[] authenticatorData,
    byte[] signature,
    Optional<byte[]> userHandle
) {
    // Canonical constructor with null checks for mandatory fields and Optional handling
    public AuthenticatorAssertionResponse(
            byte[] clientDataJSON,
            byte[] authenticatorData,
            byte[] signature,
            Optional<byte[]> userHandle) {
        this.clientDataJSON = Objects.requireNonNull(clientDataJSON, "clientDataJSON must not be null");
        this.authenticatorData = Objects.requireNonNull(authenticatorData, "authenticatorData must not be null");
        this.signature = Objects.requireNonNull(signature, "signature must not be null");
        this.userHandle = userHandle == null ? Optional.empty() : userHandle.map(uh -> Arrays.copyOf(uh, uh.length)); //Defensive copy for byte[] in Optional
    }

    // Convenience constructor for when userHandle is provided directly
    public AuthenticatorAssertionResponse(
            byte[] clientDataJSON,
            byte[] authenticatorData,
            byte[] signature,
            byte[] userHandle) {
        this(clientDataJSON, authenticatorData, signature, Optional.ofNullable(userHandle));
    }
    
    // Convenience constructor for when userHandle is absent
    public AuthenticatorAssertionResponse(
            byte[] clientDataJSON,
            byte[] authenticatorData,
            byte[] signature) {
        this(clientDataJSON, authenticatorData, signature, Optional.empty());
    }


    // Override equals and hashCode for byte[] fields
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticatorAssertionResponse that = (AuthenticatorAssertionResponse) o;
        return Arrays.equals(clientDataJSON, that.clientDataJSON) &&
               Arrays.equals(authenticatorData, that.authenticatorData) &&
               Arrays.equals(signature, that.signature) &&
               userHandle.map(Arrays::hashCode).equals(that.userHandle.map(Arrays::hashCode)); // Compare content of Optional<byte[]>
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(clientDataJSON);
        result = 31 * result + Arrays.hashCode(authenticatorData);
        result = 31 * result + Arrays.hashCode(signature);
        result = 31 * result + userHandle.map(Arrays::hashCode).orElse(0);
        return result;
    }

    // Override toString for better readability of byte[] fields
    @Override
    public String toString() {
        return "AuthenticatorAssertionResponse{" +
               "clientDataJSON_length=" + (clientDataJSON != null ? clientDataJSON.length : "null") +
               ", authenticatorData_length=" + (authenticatorData != null ? authenticatorData.length : "null") +
               ", signature_length=" + (signature != null ? signature.length : "null") +
               ", userHandle_present=" + userHandle.isPresent() +
               (userHandle.isPresent() ? ", userHandle_length=" + userHandle.get().length : "") +
               '}';
    }
}
