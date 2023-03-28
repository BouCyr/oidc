package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeEndpointParams;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ParamsDB {

    //TODO [20/03/2023] Cleanup/regular cleanup / max life time before error

    private static final ParamsDB instance = new ParamsDB();
    public static ParamsDB getInstance() {return instance;}
    private ParamsDB(){ }


    private final Map<String, AuthorizeEndpointParams> store = new HashMap<>();
    public String store(AuthorizeEndpointParams p){
        String key = UUID.randomUUID().toString();
        store.put(key, p);
        return key;
    }

    public Optional<AuthorizeEndpointParams> retrieve(String key){

        return Optional.ofNullable(store.remove(key));
    }

}
