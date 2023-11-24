package app.cbo.oidc.java.server.http;

import app.cbo.oidc.java.server.scan.Injectable;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.logging.Logger;

@Injectable
public class NotFoundHandler implements HttpHandlerWithPath {

    public static final String ROOT = "/";

    private static final Logger LOGGER = Logger.getLogger(NotFoundHandler.class.getCanonicalName());

    @Override
    public String path() {
        return ROOT;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        LOGGER.info("404 on " + exchange.getRequestURI().toString());
        exchange.sendResponseHeaders(404, 0);
        exchange.getResponseBody().flush();
        exchange.getResponseBody().close();
        return;
    }
}
