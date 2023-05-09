package app.cbo.oidc.java.server.endpoints;

import app.cbo.oidc.java.server.backends.ongoingAuths.OngoingAuthsStorer;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeParams;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Deprecated //better to craft dedicated interaction for each case
public record RedirectInteraction(
        OngoingAuthsStorer ongoingAuthsStorer,
        String uri,
        AuthorizeParams originalParams,
        Map<String, String> redirectParams,
        boolean internal) implements Interaction {


    public static RedirectInteraction internal(OngoingAuthsStorer ongoingAuthsStorer, String uri, AuthorizeParams originalParams, Map<String, String> redirectParams) {
        return new RedirectInteraction(ongoingAuthsStorer, uri, originalParams, redirectParams, true);
    }

    public static RedirectInteraction external(OngoingAuthsStorer ongoingAuthsStorer, String uri, Map<String, String> redirectParams) {
        return new RedirectInteraction(ongoingAuthsStorer, uri, null, redirectParams, false);
    }

    public void handle(@NotNull HttpExchange exchange) throws IOException {


        //copy in a new Hashmap, in case this.redirectParams() is RO.
        Map<String, String> actualRedirectParams = new HashMap<>(this.redirectParams());
        if (this.internal()) {
            //if internal redirect, store the initial authentication request sent by the client somewhere, to be able to carry on
            var ongoing = this.ongoingAuthsStorer().store(originalParams);
            actualRedirectParams.put("ongoing", ongoing.getOngoingAuthId());
        }


        var queryString = actualRedirectParams.entrySet()
                .stream().map(entry -> entry.getKey()+"="+entry.getValue())
                .collect(Collectors.joining("&"));


        exchange.getResponseHeaders().add("Location", uri+"?"+queryString);
        exchange.sendResponseHeaders(HttpCode.FOUND.code(), 0);

        exchange.getResponseBody().flush();
        exchange.getResponseBody().close();
        return;

    }
}
