package app.cbo.oidc.java.server.datastored;

import java.util.function.Supplier;

@FunctionalInterface
public interface SessionId extends Supplier<String> {

    default String getSessionId(){
        return this.get();
    }
}
