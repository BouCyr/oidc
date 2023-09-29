package app.cbo.oidc.java.server.http.consent;

import app.cbo.oidc.java.server.backends.ongoingAuths.OngoingAuthsFinder;
import app.cbo.oidc.java.server.backends.sessions.SessionFinder;
import app.cbo.oidc.java.server.datastored.OngoingAuthId;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.http.AuthErrorInteraction;
import app.cbo.oidc.java.server.http.HttpHandlerWithPath;
import app.cbo.oidc.java.server.utils.Cookies;
import app.cbo.oidc.java.server.utils.ParamsHelper;
import app.cbo.oidc.java.server.utils.Utils;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

import static app.cbo.oidc.java.server.utils.ParamsHelper.extractParams;

public class ConsentHandler implements HttpHandlerWithPath {

    private final static Logger LOGGER = Logger.getLogger(ConsentHandler.class.getCanonicalName());

    public static final String CONSENT_ENDPOINT = "/consent";

    private final OngoingAuthsFinder ongoingAuthsFinder;
    private final ConsentEndpoint consentEndpoint;
    private final SessionFinder sessionFinder;


    public ConsentHandler(
            OngoingAuthsFinder ongoingAuthsFinder,
            ConsentEndpoint consentEndpoint,
            SessionFinder sessionFinder) {
        this.ongoingAuthsFinder = ongoingAuthsFinder;
        this.consentEndpoint = consentEndpoint;
        this.sessionFinder = sessionFinder;
    }

    @Override
    public String path() {
        return CONSENT_ENDPOINT;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            Map<String, Collection<String>> raw = extractParams(exchange);

            ConsentParams params;
            if (!raw.containsKey(ConsentParams.BACK)) {
                LOGGER.info("Finding params from the redirect form");
                params = new ConsentParams(this.ongoingAuthsFinder, raw);
            } else {
                LOGGER.info("Finding params from the consent form");
                params = readConsentParams(raw);
            }

            var cookies = Cookies.parseCookies(exchange);
            var sessionId = Cookies.findSessionCookie(cookies);

            Optional<Session> session = sessionId.isEmpty() ? Optional.empty() : this.sessionFinder.find(sessionId.get());

            this.consentEndpoint
                    .treatRequest(session, params)
                    .handle(exchange);


        } catch (AuthErrorInteraction authError) {
            authError.handle(exchange);
            return;
        } catch (Exception e) {
            new AuthErrorInteraction(AuthErrorInteraction.Code.server_error, "?").handle(exchange);
            return;
        }
    }

    private ConsentParams readConsentParams(Map<String, Collection<String>> raw) throws AuthErrorInteraction {
        ConsentParams params;
        var ongoingId = ParamsHelper.singleParam(raw.get(ConsentParams.ONGOING))
                .orElseThrow(() -> new AuthErrorInteraction(AuthErrorInteraction.Code.server_error, "Cannot retrieve current authorization in request"));

        var ongoingRequest = this.ongoingAuthsFinder.find(OngoingAuthId.of(ongoingId))
                .orElseThrow(() -> new AuthErrorInteraction(AuthErrorInteraction.Code.server_error, "Cannot retrieve current authorization in storage"));

        var requested = Set.copyOf(ongoingRequest.scopes());
        var consentGiven = !Utils.isBlank(raw.get("OK")) && Boolean.parseBoolean(raw.get("OK").iterator().next());

        params = new ConsentParams(
                requested, //requested (one/checkbox)
                consentGiven, //GIVEN (one per checkbox @ on)
                ongoingRequest.clientId().orElseThrow(() -> new AuthErrorInteraction(AuthErrorInteraction.Code.unauthorized_client, "wat?")),
                ongoingRequest,
                true);
        return params;
    }
}
