package app.cbo.oidc.java.server.backends.codes;

import app.cbo.oidc.java.server.backends.filesystem.FileSpecifications;
import app.cbo.oidc.java.server.backends.filesystem.FileStorage;
import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Code;
import app.cbo.oidc.java.server.datastored.CodeData;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;
import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.utils.Utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;


/**
 * This class represents a file system-based code storage.
 * It implements the Codes interface and provides methods for code creation and consumption.
 * The codes are stored in a file system, with the code as the key and the code data as the value.
 */
@Injectable
public record FSCodes(FileStorage userDataFileStorage) implements Codes {

    private final static Logger LOGGER = Logger.getLogger(FSCodes.class.getCanonicalName());

    /**
     * This method is used to consume a code, returning the needed data.
     * It checks if the provided code, client ID, and redirect URI match the ones stored in the file system.
     * If the match is successful, it deletes the code file and returns the code data.
     * If the match is unsuccessful, it returns an empty Optional.
     *
     * @param code        The code being received by the server for validation.
     * @param clientId    The client ID that sent the code back.
     * @param redirectUri The redirect URI sent with the validation.
     * @return            The data stored server-side for this code at generation (userId, sessionId, scopes requested and nonce) ; EMPTY if the code is invalid, or not recognized by the server.
     */
    @NotNull
    @Override
    public Optional<CodeData> consume(@NotNull Code code, @NotNull ClientId clientId, @NotNull String redirectUri) {

        var file = FileSpecifications.in("codes", clientId.get())
                .fileName(code.getCode());
        Map<String, String> contents = null;
        try {
            contents = this.userDataFileStorage().readMap(file).orElseThrow(() -> new IOException("File not found"));
        } catch (IOException e) {
            LOGGER.info("File not found for code "+code.getCode());
            return Optional.empty();
        }

        if (redirectUri == null || !redirectUri.equals(contents.get("redirectUri"))) {
            LOGGER.info("Code was not issued for this redirectUri");
            return Optional.empty();
        }


        var codeData = new CodeData(
                UserId.of(contents.get("userId")),
                SessionId.of(contents.get("sessionId")),
                List.of(contents.get("scopes").split(";")),
                contents.get("nonce")
        );


        try {
            this.userDataFileStorage().delete(file);
        } catch (IOException e) {
            LOGGER.warning("could not delete code file");
        }
        return Optional.of(codeData);
    }

    /**
     * This method is used to create a new code for a specific user, client, session, and redirect URI.
     * It generates a unique code, stores it in the file system along with the provided data, and returns the code.
     * If any of the required parameters are null or blank, it throws a NullPointerException.
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
    @NotNull
    @Override
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

        var file = FileSpecifications.in("codes", clientId.get())
                .fileName(code.getCode());

        try {
            this.userDataFileStorage().writeMap(file,
                    Map.of(
                            "userId", userId.getUserId(),
                            "redirectUri", redirectUri,
                            "sessionId", sessionId.getSessionId(),
                            "nonce", nonce == null ? "" : nonce,
                            "scopes", String.join(";", scopes)
                    ));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return code;
    }
}
