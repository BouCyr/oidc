package app.cbo.oidc.java.server.endpoints.consent;

import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeHandler;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeParams;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public record ConsentGivenInteraction(AuthorizeParams params) implements Interaction {

    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Location", AuthorizeHandler.AUTHORIZE_ENDPOINT + "?" + params.toQueryString());
        exchange.sendResponseHeaders(HttpCode.FOUND.code(), 0);
        exchange.getResponseBody().flush();
        exchange.getResponseBody().close();
        return;
    }
}
