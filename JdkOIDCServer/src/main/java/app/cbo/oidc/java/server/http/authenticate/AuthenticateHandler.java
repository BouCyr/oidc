package app.cbo.oidc.java.server.http.authenticate;

import app.cbo.oidc.java.server.backends.sessions.SessionFinder;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.http.AuthErrorInteraction;
import app.cbo.oidc.java.server.http.HttpHandlerWithPath;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.Cookies;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static app.cbo.oidc.java.server.utils.ParamsHelper.extractParams;

public class AuthenticateHandler implements HttpHandlerWithPath {

    private final static Logger LOGGER = Logger.getLogger(AuthenticateHandler.class.getCanonicalName());
    public static final String AUTHENTICATE_ENDPOINT = "/login";

    private final AuthenticateEndpoint endpoint;
    private final SessionFinder sessionFinder;

    public AuthenticateHandler(AuthenticateEndpoint endpoint, SessionFinder sessionFinder) {
        this.endpoint = endpoint;
        this.sessionFinder = sessionFinder;
    }

    @Override
    public String path() {
        return AUTHENTICATE_ENDPOINT;
    }

    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {

        try {

            Map<String, Collection<String>> params = extractParams(exchange);

            var cookies = Cookies.parseCookies(exchange);
            var sessionId = Cookies.findSessionCookie(cookies);

            Optional<Session> session = sessionId.isEmpty() ? Optional.empty() : this.sessionFinder.find(sessionId.get());

            var result = this.endpoint.treatRequest(session, params);
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