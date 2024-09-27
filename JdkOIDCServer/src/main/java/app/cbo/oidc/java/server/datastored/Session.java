package app.cbo.oidc.java.server.datastored;

import app.cbo.oidc.java.server.credentials.AuthenticationMode;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.jsr305.NotNull;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public record Session(@NotNull String id,
                      @NotNull UserId userId,
                      @NotNull LocalDateTime authTime,
                      @NotNull LocalDateTime refreshTime,
                      @NotNull EnumSet<AuthenticationMode> authentications,
                      @NotNull Set<String> scopes) {

    public Session(@NotNull UserId user,
                   @NotNull EnumSet<AuthenticationMode> validatedAuthentication,
                   @NotNull Set<String> scopes) {
        this(UUID.randomUUID().toString(),
                user,
                LocalDateTime.now(),
                LocalDateTime.now(),
                validatedAuthentication,
                scopes);
    }

    public static Session refreshed(@NotNull Session original) {
        return new Session(
                original.id(),
                original.userId(),
                original.authTime(),
                LocalDateTime.now(),
                original.authentications(),
                original.scopes());
    }
}
