package app.cbo.oidc.java.server.backends.users;

import app.cbo.oidc.java.server.credentials.pwds.PasswordEncoder;
import app.cbo.oidc.java.server.datastored.user.User;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;
import app.cbo.oidc.java.server.scan.Injectable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Injectable("mem")
public class MemUsers implements Users {


    private final Map<String, User> users = new ConcurrentHashMap<>();

    private final PasswordEncoder passwordEncoder;

    public MemUsers(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    @NotNull
    public Optional<User> find(@NotNull UserId userId) {
        return Optional.ofNullable(this.users.get(userId.getUserId()));
    }

    public boolean update(@NotNull User user) {
        if (this.users.containsKey(user.sub())) {
            users.put(user.sub(), user);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public UserId create(@NotNull String login, @Nullable String clearPwd, @Nullable String totpKey) {

        if (this.find(UserId.of(login)).isPresent()) {
            throw new RuntimeException(LOGIN_ALREADY_EXISTS);
        }
        User newUser = new User(login, this.passwordEncoder.encode(clearPwd), totpKey);
        this.users.put(newUser.sub(), newUser);
        return UserId.of(newUser.sub());
    }


}
