package app.cbo.oidc.java.server.webauthn.core;

import java.util.Optional;

/**
 * Criteria for authenticator selection during registration.
 * All fields are optional.
 *
 * @param authenticatorAttachment Specifies the authenticator attachment modality (e.g., "platform", "cross-platform").
 * @param requireResidentKey Indicates if the Relying Party requires a client-side discoverable credential.
 * @param residentKey Specifies the Relying Party's preference for client-side discoverable credentials (e.g., "discouraged", "preferred", "required").
 * @param userVerification Specifies the Relying Party's requirements for user verification (e.g., "required", "preferred", "discouraged").
 */
public record AuthenticatorSelectionCriteria(
    Optional<String> authenticatorAttachment,
    Optional<Boolean> requireResidentKey,
    Optional<String> residentKey,
    Optional<String> userVerification
) {
    // Canonical constructor to ensure all fields are handled as Optional
    public AuthenticatorSelectionCriteria(
            Optional<String> authenticatorAttachment,
            Optional<Boolean> requireResidentKey,
            Optional<String> residentKey,
            Optional<String> userVerification) {
        this.authenticatorAttachment = authenticatorAttachment == null ? Optional.empty() : authenticatorAttachment;
        this.requireResidentKey = requireResidentKey == null ? Optional.empty() : requireResidentKey;
        this.residentKey = residentKey == null ? Optional.empty() : residentKey;
        this.userVerification = userVerification == null ? Optional.empty() : userVerification;
    }

    // Builder pattern might be useful here for easier construction due to all optional fields,
    // but sticking to record features for now.
    // Example: public static class Builder { ... }

    // Convenience constructor for common cases, e.g., only authenticatorAttachment
    public AuthenticatorSelectionCriteria(String authenticatorAttachment, Boolean requireResidentKey, String residentKey, String userVerification) {
        this(Optional.ofNullable(authenticatorAttachment), Optional.ofNullable(requireResidentKey), Optional.ofNullable(residentKey), Optional.ofNullable(userVerification));
    }
    
    // Default constructor for no criteria
    public AuthenticatorSelectionCriteria() {
        this(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }
}
