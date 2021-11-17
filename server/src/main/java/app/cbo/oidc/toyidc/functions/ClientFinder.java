package app.cbo.oidc.toyidc.functions;

import app.cbo.oidc.toyidc.data.Client;

import java.util.Optional;

@FunctionalInterface
public interface ClientFinder {
    Optional<Client> locate(String clientId);
}
