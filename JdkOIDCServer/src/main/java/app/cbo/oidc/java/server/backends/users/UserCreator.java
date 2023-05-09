package app.cbo.oidc.java.server.backends.users;

import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;

public interface UserCreator {
    UserId create(@NotNull String login, @Nullable String clearPwd, @Nullable String totpKey);
}
