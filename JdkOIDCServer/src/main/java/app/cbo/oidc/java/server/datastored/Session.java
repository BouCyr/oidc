package app.cbo.oidc.java.server.datastored;

import app.cbo.oidc.java.server.credentials.AuthenticationMode;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.jsr305.NotNull;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.UUID;

public record Session(String id, UserId userId, LocalDateTime authTime, LocalDateTime refreshTime, EnumSet<AuthenticationMode> authentications) {

    public Session(@NotNull UserId user, EnumSet<AuthenticationMode> validatedAuthentication) {
        this(UUID.randomUUID().toString(),
                user,
                LocalDateTime.now(),
                LocalDateTime.now(),
                validatedAuthentication);
    }

    public static Session refreshed(@NotNull Session original) {
        return new Session(original.id(), original.userId(), original.authTime(), LocalDateTime.now(), original.authentications());
    }
}
