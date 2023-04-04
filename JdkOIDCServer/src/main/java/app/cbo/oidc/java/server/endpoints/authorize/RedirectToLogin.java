package app.cbo.oidc.java.server.endpoints.authorize;

import app.cbo.oidc.java.server.backends.OngoingAuths;
import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.endpoints.authenticate.AuthenticateEndpointParams;
import app.cbo.oidc.java.server.utils.HttpCode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public record RedirectToLogin(AuthorizeEndpointParams params) implements Interaction {

    //TODO [03/04/2023] ACR(s?) requested


    @Override
    public void handle(HttpExchange exchange) throws IOException {


        String ongoingAuthId = OngoingAuths.getInstance().store(params);

        exchange.getResponseHeaders().add("Location", "/login?"+ AuthenticateEndpointParams.ONGOING+"=" + ongoingAuthId);
        exchange.sendResponseHeaders(HttpCode.FOUND.code(), 0);
        exchange.getResponseBody().flush();
        exchange.getResponseBody().close();
        return;

    }
}
