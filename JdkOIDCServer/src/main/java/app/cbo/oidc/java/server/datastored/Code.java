package app.cbo.oidc.java.server.datastored;

import java.util.function.Supplier;

@FunctionalInterface
public interface Code extends Supplier<String> {

    default String getCode(){
        return this.get();
    }

    /**
     * Returns a basic impl of Code
     */
    static Code of(String value){
        return new Simple(value);
    }
    /**
     * Basic impl
     */
    record Simple(String value) implements Code{
        @Override
        public String get() {
            return value();
        }
    }
}
