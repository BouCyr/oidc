package app.cbo.oidc.java.server.webauthn.storage;

import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.datastored.user.WebAuthnCredential;
import app.cbo.oidc.java.server.webauthn.core.PublicKeyCredentialDescriptor;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.ArrayList; // Required for Collections.synchronizedList

public class InMemoryCredentialRepository implements CredentialRepository {

    private static final Logger LOGGER = Logger.getLogger(InMemoryCredentialRepository.class.getName());
    private static final Base64.Decoder B64URL_DECODER = Base64.getUrlDecoder();
    // private static final Base64.Encoder B64URL_ENCODER = Base64.getUrlEncoder().withoutPadding(); // Not used in this impl


    // Stores credentials by their Base64URL encoded credential ID
    private final Map<String, WebAuthnCredential> credentialsById = new ConcurrentHashMap<>();
    // Stores credentials by their Base64URL encoded user handle (assuming userHandle is also Base64URL)
    private final Map<String, WebAuthnCredential> credentialsByUserHandle = new ConcurrentHashMap<>();
    // Stores a mapping from UserId (app specific) to a list of credential IDs (Base64URL encoded)
    private final Map<UserId, List<String>> userCredentialIds = new ConcurrentHashMap<>();


    @Override
    public void saveCredential(WebAuthnCredential credential) {
        if (credential == null || credential.getCredentialId() == null || credential.getUserId() == null) {
            LOGGER.log(Level.WARNING, "Attempted to save invalid credential (null credential, credentialId, or userId).");
            // Or throw IllegalArgumentException based on desired strictness
            // throw new IllegalArgumentException("Credential, Credential ID, and User ID must not be null.");
            return; 
        }
        
        String credIdBase64Url = credential.getCredentialId(); // Assuming it's already Base64URL
        credentialsById.put(credIdBase64Url, credential);
        
        if (credential.getUserHandle() != null) {
            // Assuming userHandle in WebAuthnCredential is also stored as Base64URL string
            credentialsByUserHandle.put(credential.getUserHandle(), credential); 
        }

        UserId appUserId = credential.getUserId(); 
        userCredentialIds.compute(appUserId, (key, list) -> {
            if (list == null) {
                // Use synchronizedList for thread-safe modifications to the list itself if multiple threads might add.
                // ConcurrentHashMap handles concurrent access to the map, but list modification needs separate sync.
                list = Collections.synchronizedList(new ArrayList<>());
            }
            // Avoid duplicate entries if this method could be called multiple times with the same credId for a user.
            if (!list.contains(credIdBase64Url)) {
                list.add(credIdBase64Url);
            }
            return list;
        });
        LOGGER.log(Level.INFO, "Saved WebAuthn credential with ID: " + credIdBase64Url + " for user: " + appUserId.toString());
    }

    @Override
    public Optional<WebAuthnCredential> findCredentialById(String credentialIdBase64Url) {
        if (credentialIdBase64Url == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(credentialsById.get(credentialIdBase64Url));
    }

    @Override
    public List<PublicKeyCredentialDescriptor> findCredentialDescriptorsByUserId(UserId userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<String> credIdsForUser = userCredentialIds.getOrDefault(userId, Collections.emptyList());
        
        // The list obtained from userCredentialIds might be a synchronized list.
        // Stream operations on synchronized lists are generally safe for reading.
        return credIdsForUser.stream()
                .map(credentialsById::get)
                .filter(Objects::nonNull)
                .map(cred -> {
                    try {
                        // Assuming credential.getCredentialId() is already Base64URL string from authenticator/client
                        byte[] rawId = B64URL_DECODER.decode(cred.getCredentialId());
                        return new PublicKeyCredentialDescriptor(
                                "public-key",
                                rawId,
                                // Ensure getTransports() returns List<String> or null
                                Optional.ofNullable(cred.getTransports()).filter(ts -> !ts.isEmpty()) 
                        );
                    } catch (IllegalArgumentException e) {
                        LOGGER.log(Level.WARNING, "Error decoding credential ID: " + cred.getCredentialId(), e);
                        return null; // Skip this credential if ID is malformed
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public void updateCredential(WebAuthnCredential credential) {
        if (credential == null || credential.getCredentialId() == null) {
            LOGGER.log(Level.WARNING, "Attempted to update invalid credential (null credential or credentialId).");
            return; 
        }
        
        String credIdBase64Url = credential.getCredentialId();
        
        // computeIfPresent ensures atomicity for the update of a single entry.
        credentialsById.computeIfPresent(credIdBase64Url, (id, oldCred) -> {
            LOGGER.log(Level.INFO, "Updating credential ID: " + id + ". Old sign count: " + oldCred.getSignatureCount() + ", New sign count: " + credential.getSignatureCount());
            return credential; // The new credential replaces the old one.
        });
        
        if (credential.getUserHandle() != null) {
             credentialsByUserHandle.computeIfPresent(credential.getUserHandle(), (uh, oldCred) -> credential);
        }
    }
    
    @Override
    public Optional<WebAuthnCredential> findCredentialByUserHandle(String userHandleBase64Url) {
         if (userHandleBase64Url == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(credentialsByUserHandle.get(userHandleBase64Url));
    }
}
