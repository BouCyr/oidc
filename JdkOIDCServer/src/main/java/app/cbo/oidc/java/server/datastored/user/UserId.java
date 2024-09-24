package app.cbo.oidc.java.server.datastored.user;

import java.util.function.Supplier;

public record UserId(String id) implements Supplier<String> {
    public static UserId of(String id) {
        return new UserId(id);
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
