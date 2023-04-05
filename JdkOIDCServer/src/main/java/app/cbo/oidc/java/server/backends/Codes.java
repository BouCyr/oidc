package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Code;
import app.cbo.oidc.java.server.datastored.UserId;
import app.cbo.oidc.java.server.jsr305.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * OIDC code storage
 */
public class Codes {

    private static final Codes instance = new Codes();
    public static Codes getInstance() {return instance;}
    private Codes() { }


    final Map<String, UserId> store = new HashMap<>();

    public @NotNull Code createFor(@NotNull UserId userId,@NotNull  ClientId clientId){

        if(userId.getUserId() == null || clientId.getClientId() == null){
            throw new NullPointerException("Input cannot be null");
        }

        Code code = Code.of(UUID.randomUUID().toString());

        store.put(this.computeKey(clientId, code), userId);

        return code;

    }

    public @NotNull Optional<UserId> consume(@NotNull Code code, @NotNull ClientId clientId){

        if(code.getCode() == null || clientId.getClientId() == null){
           return Optional.empty();
        }

        return Optional.ofNullable(this.store.remove(this.computeKey(clientId, code)));
    }

    //code is only valid for ONE client_id
    private @NotNull  String computeKey(@NotNull ClientId clientId,@NotNull  Code code) {
        return code.getCode() + "_for_" + clientId.getClientId();
    }
}
