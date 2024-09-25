package app.cbo.oidc.java.server.datastored;

import java.util.function.Supplier;

public record Code(String code) implements Supplier<String> {
    public static Code of(String val) {
        return new Code(val);
    }

    @Override
    public String get() {
        return code();
    }

    @Override
    public String toString() {
        return code();
    }
}
