package app.cbo.oidc.java.server.webauthn.core;

import java.util.Arrays;
import java.util.Optional;

/**
 * Represents the User entity for WebAuthn operations.
 * @param id A globally unique identifier for the user account (byte array).
 * @param name A human-palatable identifier for the user account (e.g., username). It is intended only for display.
 * @param displayName A human-palatable name for the user account, intended for display.
 * @param icon A URL which resolves to an image containing a FIDO Alliance logo, etc. for the user (optional).
 */
public record UserEntity(
    byte[] id,
    String name,
    String displayName,
    Optional<String> icon
) {
    // Constructor for optional icon
    public UserEntity(byte[] id, String name, String displayName, String icon) {
        this(id, name, displayName, Optional.ofNullable(icon));
    }

    // Canonical constructor ensures icon is Optional
    public UserEntity(byte[] id, String name, String displayName, Optional<String> icon) {
        this.id = id; // byte[] is mutable, but record components are final. Defensive copy if needed elsewhere.
        this.name = name;
        this.displayName = displayName;
        this.icon = icon == null ? Optional.empty() : icon;
    }

    // Overloaded constructor for when icon is not provided
    public UserEntity(byte[] id, String name, String displayName) {
        this(id, name, displayName, Optional.empty());
    }

    // Override equals and hashCode for byte[] id field
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserEntity that = (UserEntity) o;
        return Arrays.equals(id, that.id) &&
               name.equals(that.name) &&
               displayName.equals(that.displayName) &&
               icon.equals(that.icon);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + displayName.hashCode();
        result = 31 * result + icon.hashCode();
        result = 31 * result + Arrays.hashCode(id);
        return result;
    }

    // Override toString for better readability of byte[] id
    @Override
    public String toString() {
        return "UserEntity{" +
               "id=" + Arrays.toString(id) + // Or a Base64 representation for conciseness
               ", name='" + name + '\'' +
               ", displayName='" + displayName + '\'' +
               ", icon=" + icon +
               '}';
    }
}
