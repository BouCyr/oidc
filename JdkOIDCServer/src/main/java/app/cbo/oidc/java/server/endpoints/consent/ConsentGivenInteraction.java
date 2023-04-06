package app.cbo.oidc.java.server.endpoints.consent;

import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class ConsentGivenInteraction implements Interaction {

    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {
        //TODO [06/04/2023] 
    }
}
