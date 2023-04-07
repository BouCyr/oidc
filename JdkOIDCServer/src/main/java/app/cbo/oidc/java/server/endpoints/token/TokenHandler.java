package app.cbo.oidc.java.server.endpoints.token;

import app.cbo.oidc.java.server.endpoints.AuthErrorInteraction;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import static app.cbo.oidc.java.server.utils.ParamsHelper.extractParams;

public class TokenHandler implements HttpHandler {

    public static final String TOKEN_ENDPOINT = "/token";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            Map<String, Collection<String>> raw = extractParams(exchange);

            TokenParams param = new TokenParams(raw);


            TokenEndpoint.getInstance()
                    .treatRequest(param)
                    .handle(exchange);


        } catch (AuthErrorInteraction authErrorInteraction) {
            authErrorInteraction.handle(exchange);
        }

    }
}
