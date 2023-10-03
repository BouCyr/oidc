package app.cbo.oidc.java.server.http.userinfo;

import app.cbo.oidc.java.server.TestHttpExchange;
import app.cbo.oidc.java.server.http.Interaction;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class UserInfoHandlerTest {

    @Test
    void path() {
        Interaction emptyInteraction = exchange -> {
            exchange.sendResponseHeaders(200, 0);
        };
        UserInfoEndpoint mock = accessToken -> emptyInteraction;
        var tested = new UserInfoHandler(mock);

        assertThat(tested.path()).isEqualTo(UserInfoHandler.USERINFO_ENDPOINT);
    }

    @Test
    void handle_no_access_token() throws IOException {
        Interaction emptyInteraction = exchange -> {
            exchange.sendResponseHeaders(200, 0);
        };

        UserInfoEndpoint mock = accessToken -> emptyInteraction;
        var tested = new UserInfoHandler(mock);

        var req = TestHttpExchange.simpleGet();

        tested.handle(req);

        assertThat(req.getResponseCode())
                .isEqualTo(401);
    }

    @Test
    void handle_with_access_token() throws IOException {
        Interaction emptyInteraction = exchange -> {
            exchange.sendResponseHeaders(4999, 0);
        };

        UserInfoEndpoint mock = accessToken -> emptyInteraction;
        var tested = new UserInfoHandler(mock);

        var req = TestHttpExchange.simpleGet();
        req.getRequestHeaders().add("Authorization", "Bearer ACCESSTOKEN");

        tested.handle(req);

        assertThat(req.getResponseCode())
                .isEqualTo(4999); //make sure the request was passed to our mock endpoint
    }
}