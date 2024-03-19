package app.cbo.oidc.java.server.backends.clients;

import app.cbo.oidc.java.server.scan.Injectable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class represents a memory-based client registry.
 * It implements the ClientRegistry interface and provides methods for client authentication and management.
 * The clients are stored in a HashMap, with the client ID as the key and the client secret as the value.
 */
@Injectable("mem")
public class MemClientRegistry implements ClientRegistry{

    /**
     * A map to store the clients. The key is the client ID and the value is the client secret.
     */
    private final Map<String, String> clients = new HashMap<>();

    /**
     * This method is used to authenticate a client.
     * It checks if the provided client ID and client secret match the ones stored in the clients map.
     *
     * @param clientId     The ID of the client to be authenticated.
     * @param clientSecret The secret of the client to be authenticated.
     * @return             Returns true if the client ID and client secret match the ones stored in the clients map, false otherwise.
     */
    @Override
    public boolean authenticate(String clientId, String clientSecret) {

        if(clientId == null)
            return false;
        return clients.getOrDefault(clientId, clientId).equals(clientSecret);
    }

    /**
     * This method is used to get the IDs of all registered clients.
     *
     * @return Returns a set containing the IDs of all registered clients.
     */
    @Override
    public Set<String> getRegisteredClients() {
        return this.clients.keySet();
    }

    /**
     * This method is used to register a new client or update an existing one.
     * It adds the provided client ID and client secret to the clients map.
     *
     * @param clientId     The ID of the client to be registered or updated.
     * @param clientSecret The secret of the client to be registered or updated.
     */
    @Override
    public void setClient(String clientId, String clientSecret) {
        this.clients.put(clientId, clientSecret);
    }
}