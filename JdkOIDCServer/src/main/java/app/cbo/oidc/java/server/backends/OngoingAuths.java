package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.datastored.OngoingAuthId;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeEndpointParams;
import app.cbo.oidc.java.server.jsr305.NotNull;

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
    @NotNull public OngoingAuthId store(@NotNull AuthorizeEndpointParams p){

        String key = UUID.randomUUID().toString();
        store.put(key, p);
        return OngoingAuthId.of(key);
    }

    @NotNull  public Optional<AuthorizeEndpointParams> retrieve(@NotNull OngoingAuthId key){
        if(key.getOngoingAuthId()==null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.remove(key.getOngoingAuthId()));
    }

}
