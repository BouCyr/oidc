package app.cbo.oidc.java.server.endpoints;

import app.cbo.oidc.java.server.endpoints.authenticate.AuthenticateEndpoint;

import java.io.InputStream;
import java.util.function.Supplier;

public class ResourceInteraction extends ResponseInteraction{

    public ResourceInteraction(String contentType, String fileName) {
        //TODO [30/03/2023] grab the classloader elsewhere
        super(contentType, () -> AuthenticateEndpoint.getInstance().getClass().getClassLoader().getResourceAsStream(fileName));
    }
}
