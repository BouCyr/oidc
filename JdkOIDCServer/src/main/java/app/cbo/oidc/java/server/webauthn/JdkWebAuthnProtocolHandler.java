package app.cbo.oidc.java.server.webauthn;

import app.cbo.oidc.java.server.datastored.user.WebAuthnCredential;
import app.cbo.oidc.java.server.webauthn.core.AuthenticatorAssertionResponse;
import app.cbo.oidc.java.server.webauthn.core.AuthenticatorSelectionCriteria;
import app.cbo.oidc.java.server.webauthn.core.CollectedClientData;
import app.cbo.oidc.java.server.webauthn.core.PublicKeyCredentialCreationOptions;
import app.cbo.oidc.java.server.webauthn.core.PublicKeyCredentialDescriptor;
import app.cbo.oidc.java.server.webauthn.core.PublicKeyCredentialContainer;
import app.cbo.oidc.java.server.webauthn.core.PublicKeyCredentialParameter;
import app.cbo.oidc.java.server.webauthn.core.PublicKeyCredentialRequestOptions;
import app.cbo.oidc.java.server.webauthn.core.RpEntity;
import app.cbo.oidc.java.server.webauthn.core.UserEntity;
import app.cbo.oidc.java.server.datastored.user.UserId; // Added for UserId.of()


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JdkWebAuthnProtocolHandler implements WebAuthnProtocolHandler {

    private static final Logger LOGGER = Logger.getLogger(JdkWebAuthnProtocolHandler.class.getName());
    private final SecureRandom secureRandom = new SecureRandom();

    // Helper: Base64URL Encoder and Decoder
    private static final Base64.Encoder B64URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64URL_DECODER = Base64.getUrlDecoder();

    private String encodeBase64Url(byte[] data) {
        return B64URL_ENCODER.encodeToString(data);
    }

    private byte[] decodeBase64Url(String str) throws WebAuthnVerificationException {
        try {
            return B64URL_DECODER.decode(str);
        } catch (IllegalArgumentException e) {
            throw new WebAuthnVerificationException("Invalid Base64URL string: " + e.getMessage(), e);
        }
    }

    // Helper: SHA-256
    private byte[] sha256(byte[] data) throws WebAuthnVerificationException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(data);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "SHA-256 algorithm not found", e);
            throw new WebAuthnVerificationException("SHA-256 not available", e);
        }
    }

    // Helper: Simple JSON parsing for CollectedClientData
    // THIS IS A VERY SIMPLISTIC PARSER. A proper JSON library would be better if allowed.
    // It assumes a flat structure and known fields for CollectedClientData.
    private CollectedClientData parseClientDataJson(byte[] clientDataJsonBytes) throws WebAuthnVerificationException {
        String json = new String(clientDataJsonBytes, StandardCharsets.UTF_8);
        try {
            String type = extractJsonField(json, "type");
            String challenge = extractJsonField(json, "challenge"); // This is Base64URL encoded
            String origin = extractJsonField(json, "origin");
            
            // crossOrigin and tokenBinding are optional and their parsing is not implemented here for simplicity.
            // Extracting them as Optional<Boolean> or Optional<Map<String, String>> with this naive parser is complex.
            Optional<Boolean> crossOrigin = Optional.empty(); // Default to empty
            String crossOriginStr = extractJsonField(json, "crossOrigin");
            if (crossOriginStr != null) {
                try {
                    crossOrigin = Optional.of(Boolean.parseBoolean(crossOriginStr));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Could not parse crossOrigin value: " + crossOriginStr, e);
                }
            }
            
            return new CollectedClientData(type, challenge, origin, crossOrigin, Optional.empty());
        } catch (Exception e) {
            throw new WebAuthnVerificationException("Failed to parse clientDataJSON: " + e.getMessage(), e);
        }
    }
    
    // Using regex for slightly more robust (but still naive) extraction.
    private String extractJsonField(String json, String fieldName) {
        // Pattern to find "fieldName":"value" or "fieldName":value (for boolean/numbers if ever needed)
        // It captures the value part. Handles strings enclosed in double quotes.
        // WARNING: Still very naive. Does not handle escaped quotes in values, nested objects, arrays etc.
        Pattern pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\":\\s*\"([^\"]*)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        // Try to match boolean true/false if it's not a string (for crossOrigin)
        pattern = Pattern.compile("\"" + Pattern.quote(fieldName) + "\":\\s*(true|false)");
        matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }

        LOGGER.log(Level.FINE, "Field not found or pattern mismatch for field: " + fieldName + " in JSON: " + json);
        return null; 
    }


    @Override
    public PublicKeyCredentialCreationOptions generateRegistrationOptions(
            String rpId, String rpName, UserEntity userEntity, List<PublicKeyCredentialDescriptor> excludeCredentials) {
        
        byte[] challenge = new byte[32];
        this.secureRandom.nextBytes(challenge);

        RpEntity rp = new RpEntity(rpId, rpName); //Icon is optional and defaults to empty
        
        List<PublicKeyCredentialParameter> pubKeyCredParams = List.of(
                new PublicKeyCredentialParameter("public-key", -7), // ES256 (ECDSA with P-256 and SHA-256)
                new PublicKeyCredentialParameter("public-key", -257) // RS256 (RSA PKCS#1 v1.5 with SHA-256)
        );
        
        // Example authenticatorSelection - prefer platform authenticator, require user verification
        AuthenticatorSelectionCriteria authenticatorSelection = new AuthenticatorSelectionCriteria(
            Optional.of("platform"), // authenticatorAttachment
            Optional.empty(),          // requireResidentKey (false by default, or not set)
            Optional.empty(),          // residentKey (e.g. "preferred" or "discouraged")
            Optional.of("required")    // userVerification
        );


        return new PublicKeyCredentialCreationOptions(
                rp,
                userEntity,
                challenge,
                pubKeyCredParams,
                Optional.of(60000L), // Timeout
                excludeCredentials == null || excludeCredentials.isEmpty() ? Optional.empty() : Optional.of(excludeCredentials),
                Optional.of(authenticatorSelection), 
                Optional.of("none"), // Attestation preference (none, indirect, direct)
                Optional.empty() // extensions
        );
    }

    @Override
    public PublicKeyCredentialRequestOptions generateAuthenticationOptions(
            String rpId, Optional<List<PublicKeyCredentialDescriptor>> allowedCredentials) {

        byte[] challenge = new byte[32];
        this.secureRandom.nextBytes(challenge);

        return new PublicKeyCredentialRequestOptions(
                challenge,
                Optional.of(60000L), // Timeout
                Optional.of(rpId),
                allowedCredentials.isPresent() && !allowedCredentials.get().isEmpty() ? allowedCredentials : Optional.empty(),
                Optional.of("preferred"), // User verification preference
                Optional.empty() // extensions
        );
    }

    @Override
    public Optional<WebAuthnCredential> verifyRegistration(
            byte[] expectedChallengeBytes, String expectedOrigin, String expectedRpId,
            PublicKeyCredentialContainer registrationResponseContainer, UserEntity userEntity) throws WebAuthnVerificationException {
        
        LOGGER.info("Attempting to verify registration...");

        if (!(registrationResponseContainer.response() instanceof AuthenticatorAttestationResponse)) {
            throw new WebAuthnVerificationException("Invalid response type in PublicKeyCredentialContainer for registration.");
        }
        AuthenticatorAttestationResponse registrationResponse = (AuthenticatorAttestationResponse) registrationResponseContainer.response();

        // 1. Parse clientDataJSON
        CollectedClientData clientData = parseClientDataJson(registrationResponse.clientDataJSON());
        
        // 2. Verify clientData
        if (!"webauthn.create".equals(clientData.type())) {
            throw new WebAuthnVerificationException("Registration failed: clientData.type is not 'webauthn.create'. Got: " + clientData.type());
        }
        if (!expectedOrigin.equals(clientData.origin())) {
            throw new WebAuthnVerificationException("Registration failed: clientData.origin mismatch. Expected: " + expectedOrigin + " Got: " + clientData.origin());
        }
        
        byte[] receivedChallengeBytes = decodeBase64Url(clientData.challenge());
        if (!MessageDigest.isEqual(expectedChallengeBytes, receivedChallengeBytes)) {
            // For debugging, do not log challenges in production
            // LOGGER.warning("Expected challenge: " + encodeBase64Url(expectedChallengeBytes));
            // LOGGER.warning("Received challenge: " + clientData.challenge());
            throw new WebAuthnVerificationException("Registration failed: clientData.challenge mismatch.");
        }

        // ---- COMPLEX PARTS DEFERRED ----
        // 3. Parse attestationObject (CBOR decoding) -> AuthenticatorData and Attestation Statement
        // 4. Determine attestation format (e.g., "packed", "fido-u2f", "none") from attestation statement
        // 5. Verify attestation statement based on format
        //    - Verify signature over authenticatorData and clientDataHash using attestation public key
        //    - Extract public key, credential ID from authenticatorData.getAttestedCredentialData()
        //    - Verify RP ID hash in authenticatorData
        //    - Check user presence/verification flags in authenticatorData
        
        LOGGER.warning("verifyRegistration: CBOR parsing, attestation verification, and cryptographic checks are NOT YET IMPLEMENTED with JDK-only logic.");
        
        // Placeholder for attested credential data parsing (this is inside AuthenticatorData)
        // AuthenticatorData authData = new AuthenticatorData(registrationResponse.attestationObject()); // This is wrong, attestationObject is CBOR map
        // byte[] rpIdHashFromAuthData = authData.getRpIdHash();
        // byte[] expectedRpIdHash = sha256(expectedRpId.getBytes(StandardCharsets.UTF_8));
        // if(!MessageDigest.isEqual(expectedRpIdHash, rpIdHashFromAuthData)){
        //      throw new WebAuthnVerificationException("RP ID hash mismatch");
        // }
        // if(!authData.isUserPresent()){
        //      throw new WebAuthnVerificationException("User Present flag not set");
        // }
        // if(!authData.hasAttestedCredentialData()){
        //      throw new WebAuthnVerificationException("Attested Credential Data flag not set");
        // }
        // Optional<byte[]> attestedCredentialDataBytes = authData.getAttestedCredentialData();
        // Then parse attestedCredentialDataBytes to get AAGUID, credentialId, publicKeyCose
        
        // For now, to allow flow, we can return an empty Optional or throw.
        // To proceed with a dummy credential for testing higher-level logic:
        // String dummyUserHandle = encodeBase64Url(userEntity.id());
        // String dummyCredentialId = registrationResponseContainer.id(); // This is base64url from client
        // return Optional.of(new WebAuthnCredential(userEntity.id(), dummyCredentialId, "dummyPublicKeyCose_Base64URL", 0, dummyUserHandle, Collections.emptyList(), false, "none", "00000000-0000-0000-0000-000000000000", "dummyName-" + System.currentTimeMillis(), System.currentTimeMillis()));
        
        // ---- START DUMMY SUCCESS LOGIC ----
        LOGGER.severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        LOGGER.severe("DUMMY REGISTRATION SUCCESS: SKIPPING ACTUAL CRYPTOGRAPHIC VERIFICATION.");
        LOGGER.severe("THIS IS INSECURE AND FOR FLOW TESTING ONLY.");
        LOGGER.severe("Missing steps: CBOR parsing of attestationObject, authenticatorData parsing,");
        LOGGER.severe("attestation statement verification (signature, certificate chain), public key extraction, RP ID hash check.");
        LOGGER.severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

        // Create a dummy credential.
        // The `credentialIdForStorage` will be the Base64URL encoding of `registrationResponseContainer.rawId()`.
        // The `userHandle` in `WebAuthnCredential` will be the Base64URL encoding of `userEntity.id()`.
        // The `UserId` for `WebAuthnCredential` will be derived from `userEntity.name()`.
        String credentialIdForStorage = encodeBase64Url(registrationResponseContainer.rawId());
        UserId appUserId = UserId.of(userEntity.name()); // Assumes UserId.of(String) exists and uses username
        String userHandleForStorage = encodeBase64Url(userEntity.id());

        WebAuthnCredential dummyCredential = new WebAuthnCredential(
                appUserId,
                credentialIdForStorage,
                "dummyPublicKeyCose_NotVerified_NeedsActualCryptoVerification", // Placeholder public key
                0L, // Initial signature count
                userHandleForStorage, // User handle
                Collections.emptyList(), // transports (optional)
                false, // backedUp (optional)
                "none-dummy", // attestationType (optional)
                "00000000-0000-0000-0000-000000000000", // dummy AAGUID (optional)
                "Dummy Authenticator " + System.currentTimeMillis(), // friendlyName (optional)
                System.currentTimeMillis() // registrationTime
        );
        return Optional.of(dummyCredential);
        // ---- END DUMMY SUCCESS LOGIC ----
    }

    @Override
    public WebAuthnCredential verifyAuthentication(
            byte[] expectedChallengeBytes, String expectedOrigin, String expectedRpId,
            List<PublicKeyCredentialDescriptor> allowCredentials, 
            PublicKeyCredentialContainer assertionResponseContainer,
            WebAuthnCredential storedCredential) throws WebAuthnVerificationException {

        LOGGER.info("Attempting to verify authentication (with DUMMY success logic)...");

        if (!(assertionResponseContainer.response() instanceof AuthenticatorAssertionResponse)) {
            throw new WebAuthnVerificationException("Invalid response type in PublicKeyCredentialContainer for authentication.");
        }
        AuthenticatorAssertionResponse assertionResponse = (AuthenticatorAssertionResponse) assertionResponseContainer.response();


        // 1. Parse clientDataJSON
        CollectedClientData clientData = parseClientDataJson(assertionResponse.clientDataJSON());

        // 2. Verify clientData
        if (!"webauthn.get".equals(clientData.type())) {
            throw new WebAuthnVerificationException("Authentication failed: clientData.type is not 'webauthn.get'. Got: " + clientData.type());
        }
        if (!expectedOrigin.equals(clientData.origin())) {
            throw new WebAuthnVerificationException("Authentication failed: clientData.origin mismatch. Expected: " + expectedOrigin + " Got: " + clientData.origin());
        }
        
        byte[] receivedChallengeBytes = decodeBase64Url(clientData.challenge());
        if (!MessageDigest.isEqual(expectedChallengeBytes, receivedChallengeBytes)) {
            throw new WebAuthnVerificationException("Authentication failed: clientData.challenge mismatch.");
        }
        
        // ---- START DUMMY SUCCESS LOGIC ----
        LOGGER.severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        LOGGER.severe("DUMMY AUTHENTICATION SUCCESS: SKIPPING ACTUAL CRYPTOGRAPHIC VERIFICATION.");
        LOGGER.severe("THIS IS INSECURE AND FOR FLOW TESTING ONLY.");
        LOGGER.severe("Missing steps: authenticatorData parsing (RP ID hash, flags, sign count),");
        LOGGER.severe("COSE public key parsing (from storedCredential.publicKeyCose()), signature verification.");
        LOGGER.severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

        // Simulate signature count update.
        long newSignCount = storedCredential.getSignatureCount() + 1;
        
        // Create a new WebAuthnCredential instance with the updated sign count.
        // This assumes WebAuthnCredential has a constructor that takes all its fields
        // and that getUserId() returns the app.cbo.oidc.java.server.datastored.user.UserId object.
        WebAuthnCredential updatedCredential = new WebAuthnCredential(
            storedCredential.getUserId(), // Assumes getUserId() returns the UserId object
            storedCredential.getCredentialId(),
            storedCredential.getPublicKeyCose(), 
            newSignCount,
            storedCredential.getUserHandle(),
            storedCredential.getTransports(),
            storedCredential.isBackedUp(),
            storedCredential.getAttestationType(), 
            storedCredential.getAaguid(), 
            storedCredential.getFriendlyName(),
            storedCredential.getRegistrationTime()
        );
        return updatedCredential;
        // ---- END DUMMY SUCCESS LOGIC ----
    }
}
