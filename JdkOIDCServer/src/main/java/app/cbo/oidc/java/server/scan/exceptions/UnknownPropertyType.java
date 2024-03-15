package app.cbo.oidc.java.server.scan.exceptions;

public class UnknownPropertyType extends RuntimeException {
    public UnknownPropertyType(Class<?> clazz) {
        super("No property converter for type '" + clazz.getCanonicalName() + "'");
    }
}
