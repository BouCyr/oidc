package app.cbo.oidc.java.server.datastored;

import java.time.LocalDateTime;
import java.util.UUID;

public record Session(String id, UserId userId, LocalDateTime authTime, LocalDateTime refreshTime) {
    //TODO [CBO] acr level

    public Session(UserId user){
        this(UUID.randomUUID().toString(),
                user,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    public Session(Session original){
        this(original.id(), original.userId(), original.authTime(), LocalDateTime.now());
    }
}
