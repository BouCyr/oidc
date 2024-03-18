package app.cbo.oidc.java.server.backends.clients;

/**
 * This is a functional interface that represents a client authenticator.
 * It contains a single method, authenticate, which takes a client ID and a client secret as parameters.
 * The authenticate method is intended to be implemented to provide the logic for authenticating a client.
 */
@FunctionalInterface
public interface ClientAuthenticator {

    /**
     * This method is used to authenticate a client.
     *
     * @param clientId     The ID of the client to be authenticated.
     * @param clientSecret The secret of the client to be authenticated.
     * @return             Returns true if the client is authenticated successfully, false otherwise.
     */
    boolean authenticate(String clientId, String clientSecret);
}