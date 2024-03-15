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

@Injectable
public record FSCodes(FileStorage userDataFileStorage) implements Codes {

    private final static Logger LOGGER = Logger.getLogger(FSCodes.class.getCanonicalName());

    @NotNull
    @Override
    public Optional<CodeData> consume(@NotNull Code code, @NotNull ClientId clientId, @NotNull String redirectUri) {

        var file = FileSpecifications.in("codes", clientId.get())
                .fileName(code.getCode());
        Map<String, String> contents = null;
        try {
            contents = this.userDataFileStorage().readMap(file).orElseThrow(() -> new IOException("File not found"));
        } catch (IOException e) {
            LOGGER.info("File not found for code " + code.getCode());
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
