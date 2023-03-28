package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.datastored.User;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CodesDB {

    private static final CodesDB instance = new CodesDB();
    public static CodesDB getInstance() {return instance;}
    private CodesDB() { }


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
