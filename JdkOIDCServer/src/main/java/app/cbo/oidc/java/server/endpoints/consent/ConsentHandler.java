package app.cbo.oidc.java.server.endpoints.consent;

import app.cbo.oidc.java.server.backends.OngoingAuths;
import app.cbo.oidc.java.server.backends.Sessions;
import app.cbo.oidc.java.server.datastored.OngoingAuthId;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.endpoints.AuthErrorInteraction;
import app.cbo.oidc.java.server.utils.Cookies;
import app.cbo.oidc.java.server.utils.ParamsHelper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static app.cbo.oidc.java.server.utils.ParamsHelper.extractParams;

public class ConsentHandler implements HttpHandler {

    public static final String CONSENT_ENDPOINT = "/consent";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            Map<String, Collection<String>> raw = extractParams(exchange);

            ConsentParams params;
            if (!raw.containsKey(ConsentParams.BACK)) {
                params = new ConsentParams(raw);
            } else {
                var ongoingId = ParamsHelper.singleParam(raw.get(ConsentParams.ONGOING))
                        .orElseThrow(() -> new AuthErrorInteraction(AuthErrorInteraction.Code.server_error, "Cannot retrieve current authorization in request"));

                var ongoingRequest = OngoingAuths.getInstance().retrieve(OngoingAuthId.of(ongoingId))
                        .orElseThrow(() -> new AuthErrorInteraction(AuthErrorInteraction.Code.server_error, "Cannot retrieve current authorization in storage"));

                var requested = Set.copyOf(ongoingRequest.scopes());


                var consentGiven = raw.entrySet()
                        .stream()
                        .filter(kv -> kv.getKey().startsWith("scope_"))
                        .filter(kv -> kv.getValue().stream().anyMatch("on"::equals))
                        .map(Map.Entry::getKey)
                        .map(k -> k.replaceFirst("scope_", ""))
                        .collect(Collectors.toSet());


                params = new ConsentParams(
                        requested, //requested (one/checkbox)
                        consentGiven, //GIVEN (one per checkbox @ on)
                        ongoingRequest.clientId().orElseThrow(() -> new AuthErrorInteraction(AuthErrorInteraction.Code.unauthorized_client, "wat?")),
                        ongoingRequest,
                        true);
            }

            var cookies = Cookies.parseCookies(exchange);
            var sessionId = Cookies.findSessionCookie(cookies);
            Optional<Session> session = sessionId.isEmpty() ? Optional.empty() : Sessions.getInstance().find(sessionId.get());

            ConsentEndpoint.getInstance()
                    .treatRequest(session, params)
                    .handle(exchange);


        } catch (AuthErrorInteraction authError) {
            authError.handle(exchange);
            return;
        }catch(Exception e){
            new AuthErrorInteraction(AuthErrorInteraction.Code.server_error, "?").handle(exchange);
            return;
        }
    }
}
