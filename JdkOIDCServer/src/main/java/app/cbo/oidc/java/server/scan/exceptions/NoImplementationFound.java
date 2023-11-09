package app.cbo.oidc.java.server.scan.exceptions;

public class NoImplementationFound extends InvalidDepTree {
    public NoImplementationFound(Class<?> building, Throwable cause) {
        super("No valid implementation found for '" + building.getCanonicalName() + "'", cause);
    }
}
