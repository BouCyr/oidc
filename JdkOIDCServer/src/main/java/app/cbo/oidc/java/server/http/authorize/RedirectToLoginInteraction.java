package app.cbo.oidc.java.server.http.authorize;

import app.cbo.oidc.java.server.datastored.OngoingAuthId;
import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.http.authenticate.AuthenticateParams;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public record RedirectToLoginInteraction(OngoingAuthId ongoingAuthId) implements Interaction {

    //TODO [03/04/2023] ACR(s?) requested


    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {


        exchange.getResponseHeaders().add("Location", "/login?" + AuthenticateParams.ONGOING + "=" + ongoingAuthId().getOngoingAuthId());
        exchange.sendResponseHeaders(HttpCode.FOUND.code(), 0);
        exchange.getResponseBody().flush();
        exchange.getResponseBody().close();
        return;

    }
}
