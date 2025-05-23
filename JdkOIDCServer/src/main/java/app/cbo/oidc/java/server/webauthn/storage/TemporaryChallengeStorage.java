package app.cbo.oidc.java.server.webauthn.storage;

import app.cbo.oidc.java.server.webauthn.core.UserEntity;
import java.util.Optional;

public interface TemporaryChallengeStorage {

    /**
     * Stores a challenge and associated user information for a given transaction ID.
     * The user entity might be partial at this stage (e.g., only username and displayName).
     * @param transactionId The unique identifier for this transaction.
     * @param challenge The challenge bytes to store.
     * @param userEntity The user entity associated with this registration attempt.
     * @param rpId The Relying Party ID for this transaction.
     */
    void storeChallengeData(String transactionId, byte[] challenge, UserEntity userEntity, String rpId);

    /**
     * Retrieves the stored challenge for a given transaction ID.
     * Implementations should typically remove the challenge after retrieval to prevent reuse.
     * @param transactionId The unique identifier for the transaction.
     * @return An Optional containing the challenge bytes if found, otherwise empty.
     */
    Optional<byte[]> retrieveChallenge(String transactionId);

    /**
     * Retrieves the stored UserEntity for a given transaction ID.
     * Implementations should typically remove the entity after retrieval.
     * @param transactionId The unique identifier for the transaction.
     * @return An Optional containing the UserEntity if found, otherwise empty.
     */
    Optional<UserEntity> retrieveUserEntity(String transactionId);
    
    /**
     * Retrieves the stored Relying Party ID for a given transaction ID.
     * Implementations should typically remove the RP ID after retrieval.
     * @param transactionId The unique identifier for the transaction.
     * @return An Optional containing the Relying Party ID string if found, otherwise empty.
     */
    Optional<String> retrieveRpId(String transactionId);

    // Consider adding a method to clean up expired challenges if not done automatically by the implementation.
    // e.g., void cleanupExpiredChallenges();
}
