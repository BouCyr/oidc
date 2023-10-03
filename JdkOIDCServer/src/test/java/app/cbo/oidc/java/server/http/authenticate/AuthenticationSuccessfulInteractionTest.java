package app.cbo.oidc.java.server.http.authenticate;

import app.cbo.oidc.java.server.TestHttpExchange;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.http.authorize.AuthorizeHandler;
import app.cbo.oidc.java.server.http.authorize.AuthorizeParams;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

class AuthenticationSuccessfulInteractionTest {

    @Test
    void handle() throws IOException {
        AuthenticationSuccessfulInteraction tested = new AuthenticationSuccessfulInteraction(
                SessionId.of("my_sessionId"),
                new AuthorizeParams(Map.of("state", List.of("STATE")))
        );

        var req = TestHttpExchange.simpleGet();
        tested.handle(req);

        Assertions.assertThat(req.getResponseCode())
                .isEqualTo(302);

        Assertions.assertThat(req.getResponseHeaders()).containsKey("location");
        Assertions.assertThat(req.getResponseHeaders().get("location")).hasSize(1);
        Assertions.assertThat(req.getResponseHeaders().get("location").get(0))
                .startsWith(AuthorizeHandler.AUTHORIZE_ENDPOINT)
                .contains("state=STATE");

        Assertions.assertThat(req.getResponseHeaders()).containsKey("set-cookie");
        Assertions.assertThat(req.getResponseHeaders().get("set-cookie")).hasSize(1);
        Assertions.assertThat(req.getResponseHeaders().get("set-cookie").get(0))
                .isEqualTo("sessionId=my_sessionId; Secure; Path=/");


    }
}