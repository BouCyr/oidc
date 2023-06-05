package app.cbo.oidc.java.server.backends.codes;

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
public class Codes implements CodeSupplier, CodeConsumer {

    private final Map<String, CodeData> store = new HashMap<>();


    /**
     * {@inheritDoc}
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
     * {@inheritDoc}
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
     * computes a unique key id for an authentication request (in order to retrieve it later)
     * TODO this is a strictly related to Map handling ; once on FS, we should use a random code, and store data in a file named with the random part.
     *
     * @deprecated change implementation when using File Storage
     */
    @Deprecated
    @NotNull
    private String computeKey(
            @NotNull Code code,
            @NotNull ClientId clientId,
            @NotNull String redirectURi) {

        return code.getCode() + "_by_" + clientId.getClientId() + "_for_" + redirectURi;
    }


}
