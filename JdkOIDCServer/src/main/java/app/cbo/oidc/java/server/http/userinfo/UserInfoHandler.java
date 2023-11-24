package app.cbo.oidc.java.server.http.userinfo;

import app.cbo.oidc.java.server.http.HttpHandlerWithPath;
import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.utils.HttpCode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Logger;

@Injectable
public class UserInfoHandler implements HttpHandlerWithPath {

    public static final String USERINFO_ENDPOINT = "/userinfo";
    private final static Logger LOGGER = Logger.getLogger(UserInfoHandler.class.getCanonicalName());


    private final UserInfoEndpoint userInfoEndpoint;

    public UserInfoHandler(UserInfoEndpoint userInfoEndpoint) {
        this.userInfoEndpoint = userInfoEndpoint;
    }

    @Override
    public String path() {
        return USERINFO_ENDPOINT;
    }

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
            this.userInfoEndpoint
                    .treatRequest(accessToken.get())
                    .handle(exchange);
        } else {
            LOGGER.info("NO access token found in Authorization header, retuning 401 status code");
            new ForbiddenResponse(HttpCode.UNAUTHORIZED, ForbiddenResponse.InternalReason.NO_TOKEN, ForbiddenResponse.NO_AUTH).handle(exchange);
        }
        return;
    }
}
