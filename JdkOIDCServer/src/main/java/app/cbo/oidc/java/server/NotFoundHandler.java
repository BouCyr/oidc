package app.cbo.oidc.java.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.logging.Logger;

public class NotFoundHandler implements HttpHandler {

    private static final Logger LOGGER = Logger.getLogger(NotFoundHandler.class.getCanonicalName());

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        LOGGER.info("404 on " + exchange.getRequestURI().toString());
        exchange.sendResponseHeaders(404, 0);
        exchange.getResponseBody().flush();
        exchange.getResponseBody().close();
        return;
    }
}
