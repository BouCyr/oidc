package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.datastored.OngoingAuthId;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeEndpointParams;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class OngoingAuths {


    //TODO [20/03/2023] Cleanup/regular cleanup / max life time before error

    private static final OngoingAuths instance = new OngoingAuths();
    public static OngoingAuths getInstance() {return instance;}
    private OngoingAuths(){ }


    private final Map<String, AuthorizeEndpointParams> store = new HashMap<>();
    public OngoingAuthId store(AuthorizeEndpointParams p){

        if(p==null){
            throw new NullPointerException("Input cannot be null");
        }
        String key = UUID.randomUUID().toString();
        store.put(key, p);
        return ()->key;
    }

    public Optional<AuthorizeEndpointParams> retrieve(OngoingAuthId key){
        if(key==null || key.get()==null) {
            throw new NullPointerException("Input cannot be null");
        }
        return Optional.ofNullable(store.remove(key.getOngoingAuthId()));
    }

}
