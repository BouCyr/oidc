package app.cbo.oidc.java.server.http.staticcontent;

import app.cbo.oidc.java.server.http.HttpHandlerWithPath;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class StaticResourceHandler implements HttpHandlerWithPath {

    public static final String STATIC = "/sc/";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        new ResourceInteraction(exchange.getRequestURI().getPath()).handle(exchange);
    }

    @Override
    public String path() {
        return STATIC;
    }
}
