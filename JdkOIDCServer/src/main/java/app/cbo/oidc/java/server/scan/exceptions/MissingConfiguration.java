package app.cbo.oidc.java.server.scan.exceptions;

public class MissingConfiguration extends InvalidDepTree {
    public MissingConfiguration(String configurationKey) {
        super("Property '" + configurationKey + "' not found in configuration, and no default was given");
    }
}
