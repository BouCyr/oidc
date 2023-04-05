package app.cbo.oidc.java.server.datastored;

import java.util.function.Supplier;

@FunctionalInterface
public interface OngoingAuthId extends Supplier<String> {

    default String getOngoingAuthId(){
        return this.get();
    }
}
