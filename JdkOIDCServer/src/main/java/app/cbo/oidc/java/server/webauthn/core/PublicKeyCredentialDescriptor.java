package app.cbo.oidc.java.server.webauthn.core;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Describes a public key credential.
 * @param type The type of credential (e.g., "public-key").
 * @param id The credential ID (byte array).
 * @param transports A list of authenticator transports that may be used to exercise this credential (optional).
 */
public record PublicKeyCredentialDescriptor(
    String type,
    byte[] id,
    Optional<List<String>> transports
) {
    // Constructor for optional transports
    public PublicKeyCredentialDescriptor(String type, byte[] id, List<String> transports) {
        this(type, id, Optional.ofNullable(transports));
    }

    // Canonical constructor ensures transports is Optional
    public PublicKeyCredentialDescriptor(String type, byte[] id, Optional<List<String>> transports) {
        this.type = type;
        this.id = id; // byte[] is mutable, but record components are final.
        this.transports = transports == null ? Optional.empty() : transports;
    }
    
    // Overloaded constructor for when transports is not provided
    public PublicKeyCredentialDescriptor(String type, byte[] id) {
        this(type, id, Optional.empty());
    }


    // Override equals and hashCode for byte[] id field
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PublicKeyCredentialDescriptor that = (PublicKeyCredentialDescriptor) o;
        return Objects.equals(type, that.type) &&
               Arrays.equals(id, that.id) &&
               Objects.equals(transports, that.transports);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(type, transports);
        result = 31 * result + Arrays.hashCode(id);
        return result;
    }

    // Override toString for better readability of byte[] id
    @Override
    public String toString() {
        return "PublicKeyCredentialDescriptor{" +
               "type='" + type + '\'' +
               ", id=" + Arrays.toString(id) + // Or a Base64 representation
               ", transports=" + transports +
               '}';
    }
}
