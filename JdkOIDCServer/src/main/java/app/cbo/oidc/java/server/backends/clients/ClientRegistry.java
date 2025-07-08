package app.cbo.oidc.java.server.backends.clients;

import app.cbo.oidc.java.server.datastored.ClientId;

import java.util.Set;
import java.util.stream.Collectors;

public interface ClientRegistry extends ClientAuthenticator {

    /**
     * Returns the list of registered clients
     *
     * @return the set of registered clients
     */
    Set<ClientId> getRegisteredClients();

    /**
     * Returns true if the client is registered
     *
     * @param clientId the client id
     * @return true if the client is registered
     */
    default boolean isClientRegistered(ClientId clientId) {
        return getRegisteredClients()
                .stream().map(ClientId::get)
                .collect(Collectors.toSet())
                .contains(clientId.get());
    }

    /**
     * Registers a client
     *
     * @param clientId     the client id
     * @param clientSecret the client secret
     */
    void setClient(ClientId clientId, String clientSecret);

    void setPublicClient(ClientId clientId);

}
