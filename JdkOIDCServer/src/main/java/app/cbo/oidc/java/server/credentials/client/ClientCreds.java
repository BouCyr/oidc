package app.cbo.oidc.java.server.credentials.client;

import app.cbo.oidc.java.server.datastored.ClientId;

public record ClientCreds(ClientId clientId, String clientSecret) {
}
