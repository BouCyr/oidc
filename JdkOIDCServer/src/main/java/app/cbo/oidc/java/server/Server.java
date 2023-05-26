package app.cbo.oidc.java.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.logging.Logger;

public class Server {


    private static final Logger LOGGER = Logger.getLogger(Server.class.getCanonicalName());
    public static final String HOST_NAME = "0.0.0.0";


    private final int port;
    private final HttpServer server;


    private final List<HttpHandlerWithPath> handlers;


    public Server(StartupArgs from, List<HttpHandlerWithPath> handlers) throws IOException {
        this.port = from.port();
        this.server = HttpServer.create(new InetSocketAddress(HOST_NAME, port), 50);
        this.handlers = handlers;

    }

    public void start() {

        LOGGER.info(String.format("Server starting on host %s and port %s ", HOST_NAME, port));

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
}
