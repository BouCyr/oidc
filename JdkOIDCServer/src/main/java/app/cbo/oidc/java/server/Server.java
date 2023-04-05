package app.cbo.oidc.java.server;

import app.cbo.oidc.java.server.endpoints.ResourceInteraction;
import app.cbo.oidc.java.server.endpoints.authenticate.AuthenticateHandler;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class Server {


    private static final Logger LOGGER = Logger.getLogger(Server.class.getCanonicalName());
    public static final String HOST_NAME = "localhost";


    private final int port;
    private final HttpServer server;




    public Server(StartupArgs from) throws IOException {
        this.port = from.port();
        this.server = HttpServer.create(new InetSocketAddress(HOST_NAME, port), 50);

    }

    public void start() {






        server.createContext(AuthorizeHandler.AUTHORIZE_ENPOINT, new AuthorizeHandler());
        server.createContext(AuthenticateHandler.AUTHENTICATE_ENDPOINT, new AuthenticateHandler());
        server.createContext("/favicon.ico", exchange -> new ResourceInteraction("image/x-icon", "favicon.ico")
                .handle(exchange));

        server.createContext("/", exchange ->{
            LOGGER.info("404 on "+exchange.getRequestURI().toString());
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().flush();
            exchange.getResponseBody().close();
            return;
        } );

        // start the server
        server.start();
        System.out.printf("Server started on host %s and port %s %n", HOST_NAME, port);
    }

    public void stop() {
        //TODO [15/03/2023] add message, maybe delay.
        this.server.stop(0);
    }

}
