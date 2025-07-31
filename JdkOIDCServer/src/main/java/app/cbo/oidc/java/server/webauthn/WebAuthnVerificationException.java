package app.cbo.oidc.java.server.webauthn;

public class WebAuthnVerificationException extends Exception {
    public WebAuthnVerificationException(String message) { super(message); }
    public WebAuthnVerificationException(String message, Throwable cause) { super(message, cause); }
}
