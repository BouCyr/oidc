package app.cbo.oidc.java.server.jwt;

public interface JWSPayloadData {
    String iss();

    long exp();
}
