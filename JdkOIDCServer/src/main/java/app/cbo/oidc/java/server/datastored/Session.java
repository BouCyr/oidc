package app.cbo.oidc.java.server.datastored;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

public record Session(String id, User user, LocalDateTime authTime, LocalDateTime refreshTime) {

    public Session(User user){
        this(UUID.randomUUID().toString(),
                user,
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    public Session(Session original){
        this(original.id(), original.user(), original.authTime(), LocalDateTime.now());
    }
}
