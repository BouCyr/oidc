package app.cbo.oidc.java.server;

import app.cbo.oidc.java.server.endpoints.ResourceInteraction;
import app.cbo.oidc.java.server.endpoints.authenticate.AuthenticateHandler;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeHandler;
import app.cbo.oidc.java.server.endpoints.consent.ConsentHandler;
import app.cbo.oidc.java.server.endpoints.jwks.JWKSHandler;
import app.cbo.oidc.java.server.endpoints.token.TokenHandler;
import app.cbo.oidc.java.server.endpoints.userinfo.UserInfoHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

public class Server {


    private static final Logger LOGGER = Logger.getLogger(Server.class.getCanonicalName());
    public static final String HOST_NAME = "0.0.0.0";


    private final int port;
    private final HttpServer server;




    public Server(StartupArgs from) throws IOException {
        this.port = from.port();
        this.server = HttpServer.create(new InetSocketAddress(HOST_NAME, port), 50);

    }

    public void start() {

        LOGGER.info(String.format("Server starting on host %s and port %s ", HOST_NAME, port));

        server.createContext(AuthorizeHandler.AUTHORIZE_ENDPOINT, new AuthorizeHandler());
        server.createContext(AuthenticateHandler.AUTHENTICATE_ENDPOINT, new AuthenticateHandler());
        server.createContext(ConsentHandler.CONSENT_ENDPOINT, new ConsentHandler());
        server.createContext(TokenHandler.TOKEN_ENDPOINT, new TokenHandler());
        server.createContext(UserInfoHandler.TOKEN_ENDPOINT, new UserInfoHandler());
        server.createContext(JWKSHandler.JWKS_ENDPOINT, new JWKSHandler());
        server.createContext("/sc/", exchange -> new ResourceInteraction(exchange.getRequestURI().getPath()).handle(exchange));

        server.createContext("/", new NotFoundHandler());

        // start the server
        server.start();
        LOGGER.info(String.format("Server started on host %s and port %s ", HOST_NAME, port));
    }


}
