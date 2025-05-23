package app.cbo.oidc.java.server.webauthn.core;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the client data collected by the browser during WebAuthn operations.
 *
 * @param type The operation type (e.g., "webauthn.create", "webauthn.get").
 * @param challenge The challenge provided by the Relying Party (Base64URL encoded).
 * @param origin The origin of the request.
 * @param crossOrigin Indicates if the request is cross-origin (optional).
 * @param tokenBinding Token binding information (optional).
 */
public record CollectedClientData(
    String type,
    String challenge, // Base64URL encoded challenge
    String origin,
    Optional<Boolean> crossOrigin,
    Optional<Map<String, String>> tokenBinding // Using Map<String, String> for simplicity
) {
    // Canonical constructor with explicit Optional handling and null checks
    public CollectedClientData(
            String type,
            String challenge,
            String origin,
            Optional<Boolean> crossOrigin,
            Optional<Map<String, String>> tokenBinding) {
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.challenge = Objects.requireNonNull(challenge, "challenge must not be null");
        this.origin = Objects.requireNonNull(origin, "origin must not be null");
        this.crossOrigin = crossOrigin == null ? Optional.empty() : crossOrigin;
        this.tokenBinding = tokenBinding == null ? Optional.empty() : tokenBinding;
    }

    // Convenience constructor for mandatory fields
    public CollectedClientData(String type, String challenge, String origin) {
        this(type, challenge, origin, Optional.empty(), Optional.empty());
    }
}
