package app.cbo.oidc.java.server.http.token;

import app.cbo.oidc.java.server.credentials.client.ClientCreds;
import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.http.HttpHandlerWithPath;
import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.http.userinfo.ForbiddenResponse;
import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.utils.HttpCode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static app.cbo.oidc.java.server.utils.ParamsHelper.extractParams;
import static app.cbo.oidc.java.server.utils.ParamsHelper.singleParam;

@Injectable
public class TokenHandler implements HttpHandlerWithPath {

    public static final String TOKEN_ENDPOINT = "/token";
    private final static Logger LOGGER = Logger.getLogger(TokenHandler.class.getCanonicalName());
    private final TokenEndpoint tokenEndpoint;

    public TokenHandler(TokenEndpoint tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    @Override
    public String path() {
        return TOKEN_ENDPOINT;
    }

    private static Optional<ClientCreds> getClientCreds(HttpExchange exchange, Map<String, Collection<String>> raw) {

        var authorizationHeader = exchange.getRequestHeaders().get("Authorization");
        if (authorizationHeader == null)
            authorizationHeader = Collections.emptyList();


        var basicCreds = authorizationHeader.stream()
                .filter(s -> s.toLowerCase(Locale.ROOT).startsWith("basic "))
                .map(s -> s.substring("basic ".length()))
                .map(s -> new String(Base64.getDecoder().decode(s.trim())))
                .filter(s -> s.contains(":"))
                .findAny();

        if (basicCreds.isPresent()) {
            String clientId = basicCreds.get().split(":")[0];
            String clientSecret = basicCreds.get().split(":")[1];
            LOGGER.info("Client credentials found in Authorization header (clientId : " + clientId + ")");
            return Optional.of(new ClientCreds(ClientId.of(clientId), clientSecret));
        } else {
            var clientIdFromBody = singleParam(raw.get("client_id"));
            var clientSecretFromBody = singleParam(raw.get("client_secret"));

            if (clientIdFromBody.isPresent() && clientSecretFromBody.isPresent()) {
                LOGGER.info("Client credentials found in body header (clientId : " + clientIdFromBody.get() + ")");
                return Optional.of(new ClientCreds(ClientId.of(clientIdFromBody.get()), clientSecretFromBody.get()));
            }
        }

        LOGGER.info("no client creds found");
        return Optional.empty();
    }

    @Override
    public void handleInternal(HttpExchange exchange) throws IOException {
        try {
            Map<String, Collection<String>> raw = extractParams(exchange);


            final var creds = getClientCreds(exchange, raw);

            TokenParams param = new TokenParams(raw);
            this.tokenEndpoint
                    .treatRequest(
                            param,
                            creds.isPresent() && creds.get().clientId() != null ? creds.get().clientId() : null,
                            creds.isPresent() && creds.get().clientSecret() != null ? creds.get().clientSecret() : null)
                    .handle(exchange);


        } catch (Exception e) {
            if (e instanceof Interaction i) {
                // the exception is able to handle the response
                i.handle(exchange);
            } else {
                LOGGER.severe("unexpected exception : ");
                e.printStackTrace();
                new ForbiddenResponse(HttpCode.SERVER_ERROR, ForbiddenResponse.InternalReason.TECHNICAL, "unexpected error in code/token handling").handle(exchange);
            }
        }

    }
}
