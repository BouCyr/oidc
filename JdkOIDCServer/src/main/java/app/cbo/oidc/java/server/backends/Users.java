package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.datastored.User;
import app.cbo.oidc.java.server.datastored.UserId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Users {
    
    private static Users instance = null;
    private Users(){ }
    public static Users getInstance() {
        if(instance == null){
          instance = new Users();
        }
        return instance;
    }

    private final Map<String, User> users = new ConcurrentHashMap<>();

    public Optional<User> find(UserId userId) {
        return Optional.ofNullable(this.users.get(userId.getUserId()));
    }

    public void store(User user) {
        this.users.put(user.sub(), user);
    }
}
