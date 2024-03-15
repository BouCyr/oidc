package app.cbo.oidc.java.server.scan.exceptions;

public abstract class InvalidDepTree extends RuntimeException {
    public InvalidDepTree(String msg) {
        super(msg);
    }

    public InvalidDepTree(String msg, Throwable cause) {
        super(msg, cause);
    }
}
