package app.cbo.oidc.java.server.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.logging.Logger;

import static java.lang.System.lineSeparator;


/**
 * JDK http server does not answer HTTP 500 on uncaught exceptions out-of-the-box
 */
public interface HttpHandlerWithPath extends HttpHandler {


    String path();


    default void handle(HttpExchange exchange) throws IOException {
        var logger = Logger.getLogger(HttpHandlerWithPath.class.getCanonicalName());

        logger.info(lineSeparator() + lineSeparator() + "Someone called " + path());
        logger.info("UA is : '" + exchange.getRequestHeaders().getFirst("User-agent") + "'");
        try {
            handleInternal(exchange);
        } catch (Exception e) {

            logger.severe("Unhandled exception");
            e.printStackTrace(System.err);
            exchange.sendResponseHeaders(500, 0);
            try (var os = exchange.getResponseBody()) {
                os.flush();
            }
        }
        logger.info("Call to " + path() + " done" + lineSeparator() + lineSeparator());
    }

    void handleInternal(HttpExchange exchange) throws IOException;
}
