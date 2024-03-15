package app.cbo.oidc.java.server.http.authorize;

import app.cbo.oidc.java.server.backends.sessions.SessionFinder;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.http.AuthErrorInteraction;
import app.cbo.oidc.java.server.http.HttpHandlerWithPath;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.utils.Cookies;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static app.cbo.oidc.java.server.utils.ParamsHelper.extractParams;


/**
 * Handles all HTTP reading/parsing,etc. for the "/authorize" url
 */
@Injectable
public class AuthorizeHandler implements HttpHandlerWithPath {

    public static final String AUTHORIZE_ENDPOINT = "/authorize";

    private final AuthorizeEndpoint endpoint;
    private final SessionFinder sessionFinder;

    public AuthorizeHandler(AuthorizeEndpoint endpoint, SessionFinder sessionFinder) {
        this.endpoint = endpoint;
        this.sessionFinder = sessionFinder;
    }

    @Override
    public String path() {
        return AUTHORIZE_ENDPOINT;
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

            new AuthErrorInteraction(AuthErrorInteraction.Code.server_error, "?").handle(exchange);
            return;
        }
    }





}
