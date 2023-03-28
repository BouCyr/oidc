package app.cbo.oidc.java.server.endpoints;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public interface Interaction {
    //TODO [17/03/2023] Define in a abstract super class "OIDCException"
    void handle(HttpExchange exchange) throws IOException;

}
