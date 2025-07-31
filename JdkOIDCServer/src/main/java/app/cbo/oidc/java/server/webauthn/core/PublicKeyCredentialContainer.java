package app.cbo.oidc.java.server.webauthn.core;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the PublicKeyCredential interface as a container for transport over the wire (e.g., JSON).
 * This structure is based on how PublicKeyCredential is typically represented in JSON.
 *
 * @param id The Base64URL encoded credential ID.
 * @param rawId The raw credential ID (byte array).
 * @param response The authenticator response (either AuthenticatorAttestationResponse or AuthenticatorAssertionResponse).
 * @param authenticatorAttachment The authenticator attachment modality (optional).
 * @param clientExtensionResults The client extension results.
 * @param type The type of the credential, typically "public-key".
 */
public record PublicKeyCredentialContainer(
    String id, // Base64URL encoded
    byte[] rawId,
    Object response, // This will be either AuthenticatorAttestationResponse or AuthenticatorAssertionResponse
    Optional<String> authenticatorAttachment,
    Map<String, Object> clientExtensionResults, // Can be complex, using Object for now
    String type
) {
    // Canonical constructor with null checks and Optional handling
    public PublicKeyCredentialContainer(
            String id,
            byte[] rawId,
            Object response,
            Optional<String> authenticatorAttachment,
            Map<String, Object> clientExtensionResults,
            String type) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.rawId = Objects.requireNonNull(rawId, "rawId must not be null");
        this.response = Objects.requireNonNull(response, "response must not be null");
        if (!(response instanceof AuthenticatorAttestationResponse || response instanceof AuthenticatorAssertionResponse)) {
            throw new IllegalArgumentException("response must be an instance of AuthenticatorAttestationResponse or AuthenticatorAssertionResponse");
        }
        this.authenticatorAttachment = authenticatorAttachment == null ? Optional.empty() : authenticatorAttachment;
        this.clientExtensionResults = Objects.requireNonNull(clientExtensionResults, "clientExtensionResults must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    // Convenience constructor with String authenticatorAttachment
     public PublicKeyCredentialContainer(
            String id,
            byte[] rawId,
            Object response,
            String authenticatorAttachment, //Direct string, will be wrapped in Optional
            Map<String, Object> clientExtensionResults,
            String type) {
        this(id, rawId, response, Optional.ofNullable(authenticatorAttachment), clientExtensionResults, type);
    }


    // Override equals and hashCode for byte[] rawId field
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicKeyCredentialContainer that = (PublicKeyCredentialContainer) o;
        return Objects.equals(id, that.id) &&
               Arrays.equals(rawId, that.rawId) &&
               Objects.equals(response, that.response) &&
               Objects.equals(authenticatorAttachment, that.authenticatorAttachment) &&
               Objects.equals(clientExtensionResults, that.clientExtensionResults) &&
               Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, response, authenticatorAttachment, clientExtensionResults, type);
        result = 31 * result + Arrays.hashCode(rawId);
        return result;
    }

    // Override toString for better readability of byte[] rawId
    @Override
    public String toString() {
        return "PublicKeyCredentialContainer{" +
               "id='" + id + '\'' +
               ", rawId=" + Arrays.toString(rawId) + // Or Base64 representation
               ", response_type=" + (response != null ? response.getClass().getSimpleName() : "null") +
               ", authenticatorAttachment=" + authenticatorAttachment +
               ", clientExtensionResults=" + clientExtensionResults +
               ", type='" + type + '\'' +
               '}';
    }
}
