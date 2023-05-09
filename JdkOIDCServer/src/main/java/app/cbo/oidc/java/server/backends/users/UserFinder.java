package app.cbo.oidc.java.server.backends.users;

import app.cbo.oidc.java.server.datastored.user.User;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.jsr305.NotNull;

import java.util.Optional;

public interface UserFinder {
    @NotNull
    Optional<User> find(@NotNull UserId userId);
}
