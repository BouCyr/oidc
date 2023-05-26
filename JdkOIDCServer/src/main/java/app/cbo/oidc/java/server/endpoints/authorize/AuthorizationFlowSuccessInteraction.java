package app.cbo.oidc.java.server.endpoints.authorize;

import app.cbo.oidc.java.server.datastored.Code;
import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public record AuthorizationFlowSuccessInteraction(AuthorizeParams params, Code code) implements Interaction {


    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {

        if (params.redirectUri().isEmpty()) {
            //check should have been done long before reaching this point
            throw new IllegalArgumentException("redirect_uri empty");
        }

        String uri = params.redirectUri().get() + "?code=" + code().getCode();
        if (params.state().isPresent()) {
            uri += "&state=" + params.state().get();
        }
        exchange.getResponseHeaders().add("Location", uri);
        exchange.sendResponseHeaders(HttpCode.FOUND.code(), 0);
        exchange.getResponseBody().flush();
        exchange.getResponseBody().close();
        return;

    }
}
