package app.cbo.oidc.java.server.datastored;

import java.util.function.Supplier;

@FunctionalInterface
public interface ClientId extends Supplier<String> {

    default String getClientId(){
        return  this.get();
    }

    /**
     * Returns a basic impl of ClientId
     */
    static ClientId of(String value){
            return new Simple(value);
    }
    /**
     * Basic impl
     */
    record Simple(String value) implements ClientId{
        @Override
        public String get() {
            return value();
        }
    }
}
