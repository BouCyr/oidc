package app.cbo.oidc.java.server.backends.users;

import app.cbo.oidc.java.server.datastored.user.User;
import app.cbo.oidc.java.server.jsr305.NotNull;

@FunctionalInterface
public interface UserUpdate {

    boolean update(@NotNull User user);
}
