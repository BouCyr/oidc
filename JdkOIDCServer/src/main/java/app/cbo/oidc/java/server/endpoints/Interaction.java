package app.cbo.oidc.java.server.endpoints;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public interface Interaction {

    void handle(HttpExchange exchange) throws IOException;

}
