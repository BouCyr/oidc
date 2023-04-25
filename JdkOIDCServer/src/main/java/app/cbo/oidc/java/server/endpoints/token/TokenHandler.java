package app.cbo.oidc.java.server.endpoints.token;

import app.cbo.oidc.java.server.endpoints.AuthErrorInteraction;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Base64;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

import static app.cbo.oidc.java.server.utils.ParamsHelper.extractParams;

public class TokenHandler implements HttpHandler {

    public static final String TOKEN_ENDPOINT = "/token";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            Map<String, Collection<String>> raw = extractParams(exchange);
            TokenParams param = new TokenParams(raw);

            var clientCreds = exchange.getRequestHeaders().get("Authorization");
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

            TokenEndpoint.getInstance()
                    .treatRequest(param, clientId, clientSecret)
                    .handle(exchange);


        } catch (AuthErrorInteraction errorInteraction) {
            errorInteraction.handle(exchange);
        } catch (Exception e) {
            System.out.println("?");
        }

    }
}
