package app.cbo.oidc.java.server.endpoints.authorize;

import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.endpoints.AuthError;
import app.cbo.oidc.java.server.utils.Cookies;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static app.cbo.oidc.java.server.utils.ParamsHelper.extractParams;


/**
 * Handles all HTTP reading/parsing,etc. for the "/authorize" url
 */
public class AuthorizeHandler implements HttpHandler {

    public static final String AUTHORIZE_ENPOINT = "/authorize";

    private final AuthorizeEndpoint endpoint = AuthorizeEndpoint.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        try {

            Map<String, Collection<String>> params = extractParams(exchange);
            Optional<SessionId> sessionId = Cookies.findSessionCookie(Cookies.parseCookies(exchange));

            var result = this.endpoint.treatRequest(sessionId.orElse(()->null), params);
            result.handle(exchange);
            return;
        }catch(AuthError error){

            error.handle(exchange);
            return;
        }catch(Exception e){

            new AuthError(AuthError.Code.server_error, "?").handle(exchange);
            return;
        }
    }





}
