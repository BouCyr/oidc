package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.datastored.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Codes {

    private static final Codes instance = new Codes();
    public static Codes getInstance() {return instance;}
    private Codes() { }


    Map<String, User> store = new HashMap<>();

    //TODO [20/03/2023] should take a session and client in parameter
    public String createFor(User user){
        String code = UUID.randomUUID().toString();

        store.put(code, user);

        return code;

    }

    public Optional<User> consume(String code){
        return Optional.ofNullable(this.store.remove(code));
    }
}
