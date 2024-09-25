package app.cbo.oidc.java.server.datastored;

import java.util.function.Supplier;

public record KeyId(String id) implements Supplier<String> {

    public static KeyId of(String id) {
        return new KeyId(id);
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