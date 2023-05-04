package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.credentials.PasswordEncoder;
import app.cbo.oidc.java.server.datastored.user.User;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;

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

    @NotNull
    public Optional<User> find(@NotNull UserId userId) {
        return Optional.ofNullable(this.users.get(userId.getUserId()));
    }





    public void create(@NotNull String login, @Nullable String clearPwd, @Nullable String totpKey) {

        if(this.find(UserId.of(login)).isPresent()){
            throw new RuntimeException("Another user with this login already exists");
        }
        User newUser = new User(login, PasswordEncoder.getInstance().encodePassword(clearPwd), totpKey);
        this.users.put(newUser.sub(), newUser);
    }


}
