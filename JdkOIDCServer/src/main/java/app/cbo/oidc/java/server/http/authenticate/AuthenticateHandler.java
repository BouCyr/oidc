package app.cbo.oidc.java.server.http.authenticate;

import app.cbo.oidc.java.server.http.AuthErrorInteraction;
import app.cbo.oidc.java.server.http.HttpHandlerWithPath;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.scan.Injectable;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

import static app.cbo.oidc.java.server.utils.ParamsHelper.extractParams;

@Injectable
public class AuthenticateHandler implements HttpHandlerWithPath {

    public static final String AUTHENTICATE_ENDPOINT = "/login";

    private final static Logger LOGGER = Logger.getLogger(AuthenticateHandler.class.getCanonicalName());
    private final AuthenticateEndpoint endpoint;

    public AuthenticateHandler(AuthenticateEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public String path() {
        return AUTHENTICATE_ENDPOINT;
    }

    @Override
    public void handleInternal(@NotNull HttpExchange exchange) throws IOException {
        //TODO [03/10/2023] I am kind of surprised we do not use the session here ?
        try {
            String path = exchange.getRequestURI().getPath();
            String webauthnAction = null;

            if (path.equals(AUTHENTICATE_ENDPOINT + "/webauthn/register/start")) {
                webauthnAction = "registerStart";
            } else if (path.equals(AUTHENTICATE_ENDPOINT + "/webauthn/register/finish")) {
                webauthnAction = "registerFinish";
            } else if (path.equals(AUTHENTICATE_ENDPOINT + "/webauthn/login/start")) {
                webauthnAction = "loginStart";
            } else if (path.equals(AUTHENTICATE_ENDPOINT + "/webauthn/login/finish")) {
                webauthnAction = "loginFinish";
            }

            Map<String, Collection<String>> params = extractParams(exchange); // Assuming this returns a mutable map

            if (webauthnAction != null) {
                // If params is immutable, it needs to be copied: e.g., params = new java.util.HashMap<>(params);
                // For now, assume extractParams returns a mutable map.
                params.put("webauthnAction", Collections.singletonList(webauthnAction));
                LOGGER.info("WebAuthn action identified by path: " + webauthnAction);
            }

            var result = this.endpoint.treatRequest(params);
            result.handle(exchange);
        } catch (AuthErrorInteraction error) {
            error.handle(exchange);
        } catch (Exception e) {
            LOGGER.info("unexpected error during authentication handling: " + e.getMessage());
            e.printStackTrace(); // Keep for debugging during development
            new AuthErrorInteraction(AuthErrorInteraction.Code.server_error, "An unexpected error occurred during authentication.").handle(exchange);
        }
    }
}
