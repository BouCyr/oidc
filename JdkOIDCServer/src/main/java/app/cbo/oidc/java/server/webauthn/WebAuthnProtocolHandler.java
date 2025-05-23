package app.cbo.oidc.java.server.webauthn;

import app.cbo.oidc.java.server.webauthn.core.UserEntity;
import app.cbo.oidc.java.server.webauthn.core.PublicKeyCredentialCreationOptions;
import app.cbo.oidc.java.server.webauthn.core.PublicKeyCredentialContainer;
import app.cbo.oidc.java.server.webauthn.core.PublicKeyCredentialRequestOptions;
import app.cbo.oidc.java.server.webauthn.core.PublicKeyCredentialDescriptor;
import app.cbo.oidc.java.server.datastored.user.WebAuthnCredential; // Assuming this path is correct based on previous tasks

import java.util.List;
import java.util.Optional;

public interface WebAuthnProtocolHandler {

    PublicKeyCredentialCreationOptions generateRegistrationOptions(
            String rpId, String rpName, UserEntity userEntity, List<PublicKeyCredentialDescriptor> excludeCredentials);

    Optional<WebAuthnCredential> verifyRegistration(
            byte[] expectedChallenge, String expectedOrigin, String expectedRpId,
            PublicKeyCredentialContainer registrationResponse, UserEntity userEntity) throws WebAuthnVerificationException;

    PublicKeyCredentialRequestOptions generateAuthenticationOptions(
            String rpId, Optional<List<PublicKeyCredentialDescriptor>> allowedCredentials);

    WebAuthnCredential verifyAuthentication(
            byte[] expectedChallenge, String expectedOrigin, String expectedRpId,
            List<PublicKeyCredentialDescriptor> allowCredentials, PublicKeyCredentialContainer assertionResponse,
            WebAuthnCredential storedCredential) throws WebAuthnVerificationException;
}
