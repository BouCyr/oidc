package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Code;
import app.cbo.oidc.java.server.datastored.CodeData;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;
import app.cbo.oidc.java.server.utils.Utils;

import java.util.*;

/**
 * OIDC code storage
 */
public class Codes {

    final Map<String, CodeData> store = new HashMap<>();

    private static final Codes instance = new Codes();

    public static Codes getInstance() {
        return instance;
    }

    private Codes() {
    }

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

    @NotNull
    public Optional<CodeData> consume(@NotNull Code code, @NotNull ClientId clientId, @NotNull String redirectUri) {

        if (code.getCode() == null || clientId.getClientId() == null || Utils.isBlank(redirectUri)) {
            return Optional.empty();
        }

        return Optional.ofNullable(this.store.remove(this.computeKey(code, clientId, redirectUri)));
    }

    //code is only valid for ONE client_id
    @NotNull
    private String computeKey(
            @NotNull Code code,
            @NotNull ClientId clientId,
            @NotNull String redirectURi) {

        return code.getCode() + "_by_" + clientId.getClientId() + "_for_" + redirectURi;
    }


}
