package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Code;
import app.cbo.oidc.java.server.datastored.UserId;

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

    public Code createFor(UserId userId, ClientId clientId){

        if(userId == null || clientId == null || userId.getUserId() == null || clientId.getClientId() == null){
            throw new NullPointerException("Input cannot be null");
        }

        String codeId = UUID.randomUUID().toString();
        Code code = ()-> codeId;

        store.put(this.computeKey(clientId, code), userId);

        return code;

    }

    public Optional<UserId> consume(Code code, ClientId clientId){

        if(code == null || clientId == null || code.getCode() == null || clientId.getClientId() == null){
            throw new NullPointerException("Input cannot be null");
        }

        return Optional.ofNullable(this.store.remove(this.computeKey(clientId, code)));
    }

    //code is only valid for ONE client_id
    private String computeKey(ClientId clientId, Code code) {
        return code.getCode() + "_for_" + clientId.getClientId();
    }
}
