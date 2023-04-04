package app.cbo.oidc.java.server.datastored;

import java.util.function.Supplier;

public interface UserId extends Supplier<String> {

    default String getUserId(){
        return this.get();
    }
}
