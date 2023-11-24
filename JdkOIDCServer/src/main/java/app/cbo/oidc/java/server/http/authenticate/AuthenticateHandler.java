package app.cbo.oidc.java.server.http.authenticate;

import app.cbo.oidc.java.server.http.AuthErrorInteraction;
import app.cbo.oidc.java.server.http.HttpHandlerWithPath;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.scan.Injectable;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

import static app.cbo.oidc.java.server.utils.ParamsHelper.extractParams;

@Injectable
public class AuthenticateHandler implements HttpHandlerWithPath {

    private final static Logger LOGGER = Logger.getLogger(AuthenticateHandler.class.getCanonicalName());
    public static final String AUTHENTICATE_ENDPOINT = "/login";

    private final AuthenticateEndpoint endpoint;

    public AuthenticateHandler(AuthenticateEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public String path() {
        return AUTHENTICATE_ENDPOINT;
    }

    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {
        // TODO [03/10/2023] I am kind of surprised we do not use the session here ?
        try {

            Map<String, Collection<String>> params = extractParams(exchange);
            var result = this.endpoint.treatRequest(params);
            result.handle(exchange);
            return;
        }catch(AuthErrorInteraction error){
            error.handle(exchange);
            return;
        }catch(Exception e){
            LOGGER.info("unexpected error");
            e.printStackTrace();
            new AuthErrorInteraction(AuthErrorInteraction.Code.server_error, "?").handle(exchange);
            return;
        }

    }
}
