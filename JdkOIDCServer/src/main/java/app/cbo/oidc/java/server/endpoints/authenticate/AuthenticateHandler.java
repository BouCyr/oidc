package app.cbo.oidc.java.server.endpoints.authenticate;

import app.cbo.oidc.java.server.endpoints.AuthError;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeEndpoint;
import app.cbo.oidc.java.server.utils.SessionHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import static app.cbo.oidc.java.server.utils.ParamsHelper.extractParams;

public class AuthenticateHandler implements HttpHandler {

    public static final String AUTHENTICATE_ENDPOINT = "/login";

    private final AuthenticateEndpoint endpoint = AuthenticateEndpoint.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        try {

            var foundSession = SessionHelper.findSessionId(exchange);
            Map<String, Collection<String>> params = extractParams(exchange);

            var result = this.endpoint.treatRequest(foundSession, params);
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
