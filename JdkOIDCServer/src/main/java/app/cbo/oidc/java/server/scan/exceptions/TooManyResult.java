package app.cbo.oidc.java.server.scan.exceptions;

public class TooManyResult extends NoSingleResult {
    public TooManyResult(int nb) {
        super(nb);
    }
}
