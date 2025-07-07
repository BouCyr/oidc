package app.cbo.oidc.java.server.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class CorsHandler implements HttpHandlerWithPath {

    private final HttpHandlerWithPath wrapped;

    public CorsHandler(HttpHandlerWithPath wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public String path() {
        return wrapped.path();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(200, -1); // No response body
            exchange.close();
        } else {
            wrapped.handle(exchange);
        }
    }
}
