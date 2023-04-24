package app.cbo.oidc.java.server.datastored;

import java.util.function.Supplier;

public interface KeyId extends Supplier<String> {

    /**
     * Returns a basic impl of ClientId
     */
    static KeyId of(String value) {
        return new Simple(value);
    }

    default String getKeyId() {
        return this.get();
    }

    /**
     * Basic impl
     */
    record Simple(String value) implements KeyId {
        @Override
        public String get() {
            return value();
        }
    }
}