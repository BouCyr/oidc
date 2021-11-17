package app.cbo.oidc.toyidc.backend;

import app.cbo.oidc.toyidc.data.Client;
import app.cbo.oidc.toyidc.functions.ClientFinder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class Clients implements ClientFinder {
    @Override
    public Optional<Client> locate(String clientId) {
        if("client".equals(clientId)){
            var client = new Client();
            client.clientId = "client";
            client.redirectUri="http://localhost:8081/oidc";
            return Optional.of(client);
        } else {
            return Optional.empty();
        }
    }
}
