package app.cbo.oidc.java.server.endpoints.authenticate;

import app.cbo.oidc.java.server.backends.sessions.Sessions;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeHandler;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeParams;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public record AuthenticationSuccessfulInteraction(SessionId sessionId, AuthorizeParams params) implements Interaction {

    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {

        exchange.getResponseHeaders().add("Set-Cookie", Sessions.SESSION_ID_COOKIE_NAME + "=" + sessionId.getSessionId() + "; Secure; Path=/");
        exchange.getResponseHeaders().add("Location", AuthorizeHandler.AUTHORIZE_ENDPOINT + "?" + params.toQueryString());
        exchange.sendResponseHeaders(HttpCode.FOUND.code(), 0);
        exchange.getResponseBody().flush();
        exchange.getResponseBody().close();
        return;

    }
}
