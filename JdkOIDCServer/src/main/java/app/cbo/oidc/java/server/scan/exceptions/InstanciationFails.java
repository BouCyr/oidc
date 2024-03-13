package app.cbo.oidc.java.server.scan.exceptions;

public class InstanciationFails extends InvalidDepTree {

    public <T> InstanciationFails(Class<T> t, Exception e) {
        super("Instanciation of '" + t.getCanonicalName() + "' failed. Dependencies were built.", e);
    }
}
