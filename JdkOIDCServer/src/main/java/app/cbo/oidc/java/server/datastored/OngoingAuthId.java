package app.cbo.oidc.java.server.datastored;

import java.util.function.Supplier;

@FunctionalInterface
public interface OngoingAuthId extends Supplier<String> {

    default String getOngoingAuthId(){
        return this.get();
    }

    /**
     * Returns a basic impl of OngoingAuthId
     */
    static OngoingAuthId of(String value){
        return new Simple(value);
    }
    /**
     * Basic impl
     */
    record Simple(String value) implements OngoingAuthId{
        @Override
        public String get() {
            return value();
        }
    }
}
