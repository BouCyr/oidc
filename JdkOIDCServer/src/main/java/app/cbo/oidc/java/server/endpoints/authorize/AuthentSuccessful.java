package app.cbo.oidc.java.server.endpoints.authorize;

import app.cbo.oidc.java.server.backends.Sessions;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.utils.HttpCode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public record AuthentSuccessful(SessionId sessionId, AuthorizeEndpointParams params) implements Interaction {

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        exchange.getResponseHeaders().add("Set-Cookie", Sessions.SESSION_ID_COOKIE_NAME+"="+sessionId.getSessionId()+"; Secure; Path=/");
        exchange.getResponseHeaders().add("Location", AuthorizeHandler.AUTHORIZE_ENPOINT+"?"+params.toQueryString());
        exchange.sendResponseHeaders(HttpCode.FOUND.code(), 0);
        exchange.getResponseBody().flush();
        exchange.getResponseBody().close();
        return;

    }
}
