package app.cbo.oidc.java.server.backends.clients;

@FunctionalInterface
public interface ClientAuthenticator {

    boolean authenticate(String clientId, String clientSecret);
}
