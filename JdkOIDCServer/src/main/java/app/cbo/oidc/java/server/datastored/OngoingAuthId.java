package app.cbo.oidc.java.server.datastored;

import java.util.function.Supplier;


public record OngoingAuthId(String id) implements Supplier<String> {

    public static OngoingAuthId of(String id) {
        return new OngoingAuthId(id);
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
