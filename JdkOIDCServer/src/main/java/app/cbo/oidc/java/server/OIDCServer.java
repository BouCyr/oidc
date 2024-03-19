package app.cbo.oidc.java.server;

import app.cbo.oidc.java.server.http.HttpHandlerWithPath;
import app.cbo.oidc.java.server.scan.Prop;
import com.sun.net.httpserver.HttpServer;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.logging.Logger;

public class OIDCServer implements Closeable {


    private static final Logger LOGGER = Logger.getLogger(OIDCServer.class.getCanonicalName());
    public static final String HOST_NAME = "0.0.0.0";


    private final int port;
    private HttpServer httpServer;


    private final List<HttpHandlerWithPath> handlers;


    public OIDCServer(@Prop("port") int port, List<HttpHandlerWithPath> handlers) {
        this.port = port;
        this.handlers = handlers;

    }

    public void start() throws IOException {

        LOGGER.info(String.format("Server starting on host %s and port %s ", HOST_NAME, port));

        this.httpServer = HttpServer.create(new InetSocketAddress(HOST_NAME, port), 50);
        handlers.forEach(handler -> {
            LOGGER.info("Adding handler '" + handler.getClass().getSimpleName() + "' matching path '" + handler.path() + "'.");
            httpServer.createContext(handler.path(), handler);
        });

        // start the server
        httpServer.start();
        LOGGER.info(String.format("Server started on host %s and port %s ", HOST_NAME, port));
    }


    public void shutdown() {
        this.httpServer.stop(0);
    }

    @Override
    public void close() {
        LOGGER.info("Closing down server");
        this.shutdown();
    }
}
