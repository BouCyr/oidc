package app.cbo.oidc.java.server.backends.codes;

import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Code;
import app.cbo.oidc.java.server.datastored.CodeData;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;
import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.utils.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * This class represents an in-memory storage for OIDC codes.
 * It implements the Codes interface and provides methods for creating and consuming codes.
 * The codes and their associated data are stored in a HashMap.
 */
@Injectable("mem")
public class MemCodes implements Codes {

    /**
     * A map to store the codes and their associated data.
     * The key is a unique identifier computed from the code, client ID, and redirect URI.
     * The value is a CodeData object containing the user ID, session ID, scopes, and nonce.
     */
    private final Map<String, CodeData> store = new HashMap<>();


    /**
     * This method is used to create a new code for a specific user, client, session, and redirect URI.
     * It generates a unique code, stores it in the map along with the provided data, and returns the code.
     *
     * @param userId      The ID of the user for whom the code is being created.
     * @param clientId    The ID of the client requesting the code.
     * @param sessionId   The ID of the session during which the code is being created.
     * @param redirectUri The redirect URI to be associated with the code.
     * @param scopes      The scopes requested by the client.
     * @param nonce       A nonce that can be used to associate a client session with an ID token and to mitigate replay attacks.
     * @return            The newly created code.
     * @throws NullPointerException if userId, clientId, or redirectUri is null or blank.
     */
    @Override
    @NotNull
    public Code createFor(@NotNull UserId userId,
                          @NotNull ClientId clientId,
                          @NotNull SessionId sessionId,
                          @NotNull String redirectUri,
                          @NotNull List<String> scopes,
                          @Nullable String nonce) {

        if (userId.getUserId() == null || clientId.getClientId() == null || Utils.isBlank(redirectUri)) {
            throw new NullPointerException("Input cannot be null");
        }

        Code code = Code.of(UUID.randomUUID().toString());

        store.put(this.computeKey(code, clientId, redirectUri), new CodeData(userId, sessionId, scopes, nonce));

        return code;

    }

    /**
     * This method is used to consume a code, returning the associated data.
     * It checks if the provided code, client ID, and redirect URI match the ones stored in the map.
     * If the match is successful, it removes the code from the map and returns the associated data.
     * If the match is unsuccessful, it returns an empty Optional.
     *
     * @param code        The code being received by the server for validation.
     * @param clientId    The client ID that sent the code back.
     * @param redirectUri The redirect URI sent with the validation.
     * @return            The data stored server-side for this code at generation (userId, sessionId, scopes requested and nonce) ; EMPTY if the code is invalid, or not recognized by the server.
     */
    @Override
    @NotNull
    public Optional<CodeData> consume(@NotNull Code code, @NotNull ClientId clientId, @NotNull String redirectUri) {

        if (code.getCode() == null || clientId.getClientId() == null || Utils.isBlank(redirectUri)) {
            return Optional.empty();
        }

        return Optional.ofNullable(this.store.remove(this.computeKey(code, clientId, redirectUri)));
    }

    /**
     * This method is used to compute a unique key for an authentication request.
     * The key is computed from the code, client ID, and redirect URI.
     *
     * @param code        The code being received by the server for validation.
     * @param clientId    The client ID that sent the code back.
     * @param redirectUri The redirect URI sent with the validation.
     * @return            A unique key computed from the code, client ID, and redirect URI.
     */
    @NotNull
    private String computeKey(
            @NotNull Code code,
            @NotNull ClientId clientId,
            @NotNull String redirectUri) {

        return code.getCode() + "_by_" + clientId.getClientId() + "_for_" + redirectUri;
    }
}
