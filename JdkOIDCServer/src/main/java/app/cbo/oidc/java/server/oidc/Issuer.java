package app.cbo.oidc.java.server.oidc;

import java.util.function.Supplier;

public interface Issuer extends Supplier<String> {

    /**
     * Returns a basic impl of Issuer
     */
    static Issuer of(String value) {
        return new Issuer.Simple(value);
    }

    default String getIssuerId() {
        return this.get();
    }

    /**
     * Basic impl
     */
    record Simple(String value) implements Issuer {
        @Override
        public String get() {
            return value();
        }
    }
}