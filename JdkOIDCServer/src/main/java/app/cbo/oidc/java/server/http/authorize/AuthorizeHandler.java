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
import java.util.logging.Logger;

import static app.cbo.oidc.java.server.utils.ParamsHelper.extractParams;


/**
 * Handles all HTTP reading/parsing,etc. for the "/authorize" url
 */
@Injectable
public class AuthorizeHandler implements HttpHandlerWithPath {

    private final static Logger LOGGER = Logger.getLogger(AuthorizeHandler.class.getCanonicalName());

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


            var cookies = Cookies.parseCookies(exchange);
            var sessionId = Cookies.findSessionCookie(cookies);
            Optional<Session> session = sessionId.isEmpty() ? Optional.empty() : this.sessionFinder.find(sessionId.get());

            Map<String, Collection<String>> params = extractParams(exchange);
            //put params in the dedicated record
            AuthorizeParams parsedParams = new AuthorizeParams(params);
            //3.1.2.2.  Authentication Request Validation
            //The Authorization Server MUST validate all the OAuth 2.0 parameters according to the OAuth 2.0 specification.
            //check their validity
            AuthorizeParams.checkParams(parsedParams);
            LOGGER.info("Request params are valid");


            var result = this.endpoint.treatRequest(session, parsedParams);
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
