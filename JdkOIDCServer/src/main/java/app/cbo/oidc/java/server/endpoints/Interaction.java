package app.cbo.oidc.java.server.endpoints;

import app.cbo.oidc.java.server.jsr305.NotNull;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public interface Interaction {

    void handle(@NotNull HttpExchange exchange) throws IOException;

}
