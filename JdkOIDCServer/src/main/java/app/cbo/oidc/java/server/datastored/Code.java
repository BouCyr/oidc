package app.cbo.oidc.java.server.datastored;

import java.util.function.Supplier;

@FunctionalInterface
public interface Code extends Supplier<String> {

    default String getCode(){
        return this.get();
    }
}
