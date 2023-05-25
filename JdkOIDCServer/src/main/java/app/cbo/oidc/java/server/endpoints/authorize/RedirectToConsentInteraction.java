package app.cbo.oidc.java.server.endpoints.authorize;

import app.cbo.oidc.java.server.datastored.OngoingAuthId;
import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.endpoints.consent.ConsentHandler;
import app.cbo.oidc.java.server.endpoints.consent.ConsentParams;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public record RedirectToConsentInteraction(OngoingAuthId ongoingAuthId, String clientId,
                                           Set<String> scopes) implements Interaction {


    public RedirectToConsentInteraction(OngoingAuthId ongoingAuthId, String clientId, Collection<String> scopes) {
        this(ongoingAuthId, clientId, Set.copyOf(scopes));
    }


    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {


        exchange.getResponseHeaders().add("Location", ConsentHandler.CONSENT_ENDPOINT
                + "?" + ConsentParams.ONGOING + "=" + ongoingAuthId().getOngoingAuthId()
                + "&" + ConsentParams.SCOPES_REQUESTED + "=" + String.join(" ", this.scopes)
                + "&" + ConsentParams.CLIENT_ID + "=" + clientId()
        );
        exchange.sendResponseHeaders(HttpCode.FOUND.code(), 0);
        exchange.getResponseBody().flush();
        exchange.getResponseBody().close();
        return;

    }
}
