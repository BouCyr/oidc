package app.cbo.oidc.java.server.http;

import app.cbo.oidc.java.server.TestHttpExchange;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class NotFoundHandlerTest {

    @Test
    void path() {

        Assertions.assertThat(new NotFoundHandler().path()).isEqualTo(NotFoundHandler.ROOT);
    }

    @Test
    void handle() throws IOException {

        var req = TestHttpExchange.simpleGet();
        new NotFoundHandler().handle(req);
        Assertions.assertThat(req.getResponseCode()).isEqualTo(404);
    }
}