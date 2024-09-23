package app.cbo.oidc.java.server.datastored;

import app.cbo.oidc.java.server.datastored.user.UserId;

import java.util.function.Supplier;

public record Code(String code) implements Supplier<String> {
    @Override public String get() { return code(); }

    @Override public String toString() { return code(); }

    public static Code of(String val){ return new Code(val);}
}
