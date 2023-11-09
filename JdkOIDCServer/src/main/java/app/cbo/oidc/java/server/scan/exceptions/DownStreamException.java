package app.cbo.oidc.java.server.scan.exceptions;

public class DownStreamException extends Exception {

    public DownStreamException(Class<?> building, InvalidDepTree cause) {
        super("Unable to build instance of '" + building.getCanonicalName() + "', some downstream dep. cannot be built", cause);
    }

    public DownStreamException(Class<?> building, DownStreamException cause) {
        super("Unable to build instance of '" + building.getCanonicalName() + "', some downstream dep. cannot be built", cause);
    }
}
