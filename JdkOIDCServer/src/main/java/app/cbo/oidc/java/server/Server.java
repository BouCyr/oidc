package app.cbo.oidc.java.server;

import app.cbo.oidc.java.server.http.HttpHandlerWithPath;
import com.sun.net.httpserver.HttpServer;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.logging.Logger;

public class Server implements Closeable {


    private static final Logger LOGGER = Logger.getLogger(Server.class.getCanonicalName());
    public static final String HOST_NAME = "0.0.0.0";


    private final int port;
    private HttpServer server;


    private final List<HttpHandlerWithPath> handlers;



    public Server(StartupArgs from, List<HttpHandlerWithPath> handlers) throws IOException {
        this.port = from.port();

        this.handlers = handlers;

    }

    public void start() throws IOException {

        LOGGER.info(String.format("Server starting on host %s and port %s ", HOST_NAME, port));
        this.server = HttpServer.create(new InetSocketAddress(HOST_NAME, port), 50);
        handlers.forEach(handler -> {
            LOGGER.info("Adding handler '" + handler.getClass().getSimpleName() + "' matching path '" + handler.path() + "'.");
            server.createContext(handler.path(), handler);
        });

        // start the server
        server.start();
        LOGGER.info(String.format("Server started on host %s and port %s ", HOST_NAME, port));
    }


    public void shutdown() {
        this.server.stop(0);
    }

    @Override
    public void close() throws IOException {
        LOGGER.info("Closing down server");
        this.shutdown();
    }
}
