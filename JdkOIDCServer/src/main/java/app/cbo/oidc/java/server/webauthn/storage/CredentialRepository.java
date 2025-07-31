package app.cbo.oidc.java.server.webauthn.storage;

import app.cbo.oidc.java.server.datastored.user.UserId; 
import app.cbo.oidc.java.server.datastored.user.WebAuthnCredential; 
import app.cbo.oidc.java.server.webauthn.core.PublicKeyCredentialDescriptor;

import java.util.List;
import java.util.Optional;

public interface CredentialRepository {

    /**
     * Saves a new WebAuthn credential to the storage.
     * This should also link the credential to the user.
     *
     * @param credential The WebAuthnCredential object to save.
     */
    void saveCredential(WebAuthnCredential credential);

    /**
     * Retrieves a WebAuthn credential by its raw credential ID (Base64URL encoded string).
     *
     * @param credentialIdBase64Url The Base64URL encoded credential ID.
     * @return An Optional containing the WebAuthnCredential if found.
     */
    Optional<WebAuthnCredential> findCredentialById(String credentialIdBase64Url);
    
    /**
     * Retrieves all WebAuthn credential descriptors associated with a given user ID.
     * Used to populate 'allowCredentials' for username-specific logins.
     *
     * @param userId The user's identifier.
     * @return A list of PublicKeyCredentialDescriptor objects.
     */
    List<PublicKeyCredentialDescriptor> findCredentialDescriptorsByUserId(UserId userId);


    /**
     * Updates an existing WebAuthn credential, typically to update the signature count.
     *
     * @param credential The WebAuthnCredential object with updated information.
     */
    void updateCredential(WebAuthnCredential credential);
    
    /**
     * (Optional but Recommended) Finds a WebAuthn credential by its user handle.
     * The user handle is often the same as the user's ID used during registration.
     * This can be useful for linking WebAuthn credentials back to application users
     * if the credential ID is not immediately known.
     *
     * @param userHandleBase64Url The Base64URL encoded user handle.
     * @return An Optional containing the WebAuthnCredential if found.
     */
    Optional<WebAuthnCredential> findCredentialByUserHandle(String userHandleBase64Url);

}
