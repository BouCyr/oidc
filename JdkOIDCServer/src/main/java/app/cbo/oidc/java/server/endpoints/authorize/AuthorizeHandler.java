package app.cbo.oidc.java.server.endpoints.authorize;

import app.cbo.oidc.java.server.endpoints.AuthError;
import app.cbo.oidc.java.server.oidc.HttpConstants;
import app.cbo.oidc.java.server.utils.QueryStringParser;
import app.cbo.oidc.java.server.utils.SessionHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

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
