package app.cbo.oidc.java.server.endpoints;

import app.cbo.oidc.java.server.endpoints.authenticate.AuthenticateEndpoint;
import app.cbo.oidc.java.server.jsr305.NotNull;

public class ResourceInteraction extends ResponseInteraction{

    public ResourceInteraction(@NotNull String contentType, @NotNull String fileName) {
        //TODO [30/03/2023] grab the classloader elsewhere
        super(contentType, () -> AuthenticateEndpoint.getInstance().getClass().getClassLoader().getResourceAsStream(fileName));
    }
}
