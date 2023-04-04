package app.cbo.oidc.java.server.endpoints.authenticate;

import app.cbo.oidc.java.server.endpoints.AuthError;
import app.cbo.oidc.java.server.utils.Cookies;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

import static app.cbo.oidc.java.server.utils.ParamsHelper.extractParams;

public class AuthenticateHandler implements HttpHandler {

    private final static Logger LOGGER = Logger.getLogger(AuthenticateHandler.class.getCanonicalName());
    public static final String AUTHENTICATE_ENDPOINT = "/login";

    private final AuthenticateEndpoint endpoint = AuthenticateEndpoint.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        try {

            Map<String, Collection<String>> params = extractParams(exchange);

            var cookies = Cookies.parseCookies(exchange);
            var sessionId = Cookies.findSessionCookie(cookies);
            var result = this.endpoint.treatRequest(sessionId.orElse(null), params);
            result.handle(exchange);
            return;
        }catch(AuthError error){
            error.handle(exchange);
            return;
        }catch(Exception e){
            LOGGER.info("unexpected error");
            e.printStackTrace();
            new AuthError(AuthError.Code.server_error, "?").handle(exchange);
            return;
        }

    }
}
