package app.cbo.oidc.java.server.datastored.user;

import app.cbo.oidc.java.server.datastored.ClientId;

import java.util.function.Supplier;

public record UserId(String id) implements Supplier<String> {
    @Override public String get() { return id(); }

    @Override public String toString() { return id(); }

    public static UserId of(String id){ return new UserId(id);}
}
