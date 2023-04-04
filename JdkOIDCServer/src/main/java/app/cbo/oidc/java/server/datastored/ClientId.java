package app.cbo.oidc.java.server.datastored;

import java.util.function.Supplier;

@FunctionalInterface
public interface ClientId extends Supplier<String> {

    default String getClientId(){
        return  this.get();
    }
}
