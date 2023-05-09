package app.cbo.oidc.java.server.endpoints.token;

import app.cbo.oidc.java.server.HttpHandlerWithPath;
import app.cbo.oidc.java.server.endpoints.AuthErrorInteraction;
import app.cbo.oidc.java.server.utils.HttpCode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.*;

import static app.cbo.oidc.java.server.utils.ParamsHelper.extractParams;

public class TokenHandler implements HttpHandlerWithPath {

    public static final String TOKEN_ENDPOINT = "/token";

    private final TokenEndpoint tokenEndpoint;

    public TokenHandler(TokenEndpoint tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    @Override
    public String path() {
        return TOKEN_ENDPOINT;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            Map<String, Collection<String>> raw = extractParams(exchange);
            TokenParams param = new TokenParams(raw);

            var clientCreds = exchange.getRequestHeaders().get("Authorization");
            if (clientCreds == null)
                clientCreds = Collections.emptyList();


            var basicCreds = clientCreds.stream()
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith("basic "))
                    .map(s -> s.substring("basic ".length()))
                    .map(s -> new String(Base64.getDecoder().decode(s.trim())))
                    .filter(s -> s.contains(":"))
                    .findAny();

            String clientId = null;
            String clientSecret = null;
            if (basicCreds.isPresent()) {
                clientId = basicCreds.get().split(":")[0];
                clientSecret = basicCreds.get().split(":")[1];
            }

            this.tokenEndpoint
                    .treatRequest(param, clientId, clientSecret)
                    .handle(exchange);


        } catch (AuthErrorInteraction | JsonError errorInteraction) {
            errorInteraction.handle(exchange);
        } catch (Exception e) {
            new JsonError(HttpCode.SERVER_ERROR, "unexpected error in code/token handling").handle(exchange);
        }

    }
}
