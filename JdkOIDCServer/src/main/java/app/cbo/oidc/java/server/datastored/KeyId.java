package app.cbo.oidc.java.server.datastored;

import app.cbo.oidc.java.server.jsr305.NotNull;

import java.util.function.Supplier;

public interface KeyId extends Supplier<String> {

    /**
     * Returns a basic impl of ClientId
     */
    static KeyId of(@NotNull String value) {
        return new Simple(value);
    }

    @NotNull
    default String getKeyId() {
        return this.get();
    }

    /**
     * Basic impl
     */
    record Simple(@NotNull String value) implements KeyId {
        @Override
        public String get() {
            return value();
        }
    }
}