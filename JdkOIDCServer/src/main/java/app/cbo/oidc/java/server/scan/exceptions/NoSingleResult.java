package app.cbo.oidc.java.server.scan.exceptions;

public abstract class NoSingleResult extends Exception {
    private final int nb;

    public NoSingleResult(int nb) {
        this.nb = nb;
    }

    public int nb() {
        return nb;
    }
}
