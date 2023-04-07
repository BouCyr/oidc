package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Code;
import app.cbo.oidc.java.server.datastored.UserId;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * OIDC code storage
 */
public class Codes {

    //TODO [06/04/2023] gestion du nonce ? de la request_id ?
    public @NotNull
    Code createFor(@NotNull UserId userId, @NotNull ClientId clientId, @NotNull String redirectUri) {

        if (userId.getUserId() == null || clientId.getClientId() == null || Utils.isBlank(redirectUri)) {
            throw new NullPointerException("Input cannot be null");
        }

        Code code = Code.of(UUID.randomUUID().toString());

        store.put(this.computeKey(code, clientId, redirectUri), userId);

        return code;

    }

    private static final Codes instance = new Codes();

    public static Codes getInstance() {
        return instance;
    }

    private Codes() {
    }


    final Map<String, UserId> store = new HashMap<>();

    public @NotNull
    Optional<UserId> consume(@NotNull Code code, @NotNull ClientId clientId, @NotNull String redirectUri) {

        if (code.getCode() == null || clientId.getClientId() == null || Utils.isBlank(redirectUri)) {
            return Optional.empty();
        }

        return Optional.ofNullable(this.store.remove(this.computeKey(code, clientId, redirectUri)));
    }

    //code is only valid for ONE client_id
    private @NotNull
    String computeKey(
            @NotNull Code code,
            @NotNull ClientId clientId,
            @NotNull String redirectURi) {

        return code.getCode() + "_by_" + clientId.getClientId() + "_for_" + redirectURi;
    }

    static record CodeData(String code, UserId target, ClientId requestBy, String requestedFor) {

    }
}
