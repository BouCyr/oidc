package app.cbo.oidc.java.server.endpoints.userinfo;

import app.cbo.oidc.java.server.utils.HttpCode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;

public class UserInfoHandler implements HttpHandler {

    public static final String TOKEN_ENDPOINT = "/userinfo";
    private final static Logger LOGGER = Logger.getLogger(UserInfoHandler.class.getCanonicalName());

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        var clientCreds = exchange.getRequestHeaders().get("Authorization");
        if (clientCreds == null) {
            clientCreds = Collections.emptyList();
        }

        LOGGER.info("someone is calling the userInfo endpoint");

        var accessToken = clientCreds.stream()
                .filter(s -> s.startsWith("Bearer "))
                .map(s -> s.substring("Bearer ".length()))
                .findAny();

        if (accessToken.isPresent()) {

            LOGGER.info("Access token found in Authorization header");
            UserInfoEndpoint.getInstance()
                    .treatRequest(accessToken.get())
                    .handle(exchange);
        } else {
            LOGGER.info("NO access token found in Authorization header, retuning 401 status code");
            new ForbiddenResponse(HttpCode.UNAUTHORIZED, ForbiddenResponse.NO_AUTH).handle(exchange);
        }
        return;
    }
}
