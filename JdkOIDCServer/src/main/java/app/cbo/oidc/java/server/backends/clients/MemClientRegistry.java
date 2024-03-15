package app.cbo.oidc.java.server.backends.clients;


import app.cbo.oidc.java.server.scan.Injectable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Injectable("mem")
public class MemClientRegistry implements ClientRegistry{

    private final Map<String, String> clients = new HashMap<>();

    @Override
    public boolean authenticate(String clientId, String clientSecret) {
        return clients.getOrDefault(clientId, UUID.randomUUID().toString()).equals(clientSecret);
    }

    @Override
    public Set<String> getRegisteredClients() {
        return this.clients.keySet();
    }

    @Override
    public void setClient(String clientId, String clientSecret) {
        this.clients.put(clientId, clientSecret);
    }
}
