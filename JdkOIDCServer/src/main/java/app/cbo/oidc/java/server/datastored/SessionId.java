package app.cbo.oidc.java.server.datastored;

import java.util.function.Supplier;

@FunctionalInterface
public interface SessionId extends Supplier<String> {

    default String getSessionId(){
        return this.get();
    }

    /**
     * Returns a basic impl of SessionId
     */
    static SessionId of(String value){
            return new Simple(value);
    }
    /**
     * Basic impl
     */
    record Simple(String value) implements SessionId{
        @Override
        public String get() {
            return value();
        }
    }
}
