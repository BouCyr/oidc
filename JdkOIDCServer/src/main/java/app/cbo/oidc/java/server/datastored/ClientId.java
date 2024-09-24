package app.cbo.oidc.java.server.datastored;

import java.util.function.Supplier;

public record ClientId(String id) implements Supplier<String> {


    public static ClientId of(String id) {
        return new ClientId(id);
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
