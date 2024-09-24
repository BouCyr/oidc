package app.cbo.oidc.java.server.datastored;

import java.util.function.Supplier;


public record SessionId(String id) implements Supplier<String> {
    public static SessionId of(String id) {
        return new SessionId(id);
    }

    @Override
    public String get() {
        return id();
    }

    @Override
    public String toString() {
        return id();
    }
}
