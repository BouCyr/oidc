package app.cbo.oidc.java.server.backends.clients;

import java.util.Set;

public interface ClientRegistry extends ClientAuthenticator{

    /**
     * Returns the list of registered clients
     * @return the list of registered clients
     */
    Set<String> getRegisteredClients();

    /**
     * Returns true if the client is registered
     * @param clientId the client id
     * @return true if the client is registered
     */
    default boolean isClientRegistered(String clientId){
        return getRegisteredClients().contains(clientId);
    }

    /**
     * Registers a client
     * @param clientId the client id
     * @param clientSecret the client secret
     */
    void setClient(String clientId, String clientSecret);

}
