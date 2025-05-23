package app.cbo.oidc.java.server.webauthn.core;

import java.util.Optional;

/**
 * Represents the Relying Party entity.
 * @param id The Relying Party identifier (e.g., domain string).
 * @param name A human-palatable name for the Relying Party.
 * @param icon A URL which resolves to an image containing a logo, etc. for the Relying Party (optional).
 */
public record RpEntity(
    String id,
    String name,
    Optional<String> icon
) {
    // Constructor for optional icon
    public RpEntity(String id, String name, String icon) {
        this(id, name, Optional.ofNullable(icon));
    }

    // Canonical constructor ensures icon is Optional
    public RpEntity(String id, String name, Optional<String> icon) {
        this.id = id;
        this.name = name;
        this.icon = icon == null ? Optional.empty() : icon;
    }

    // Overloaded constructor for when icon is not provided
    public RpEntity(String id, String name) {
        this(id, name, Optional.empty());
    }
}
