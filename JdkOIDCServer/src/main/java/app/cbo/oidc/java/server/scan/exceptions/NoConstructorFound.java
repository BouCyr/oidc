package app.cbo.oidc.java.server.scan.exceptions;

public class NoConstructorFound extends InvalidDepTree {
    public NoConstructorFound(Class<?> building) {
        super("No valid constructor found for '" + building.getCanonicalName() + "'");
    }
}
