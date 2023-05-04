package app.cbo.oidc.java.server.datastored.user;

import java.util.function.Supplier;

@FunctionalInterface
public interface UserId extends Supplier<String> {

    default String getUserId(){
        return this.get();
    }

    /**
     * Returns a basic impl of UserId
     */
    static UserId of(String value){
        return new Simple(value);
    }
    /**
     * Basic impl
     */
    record Simple(String value) implements UserId{
        @Override
        public String get() {
            return value();
        }
    }
}
