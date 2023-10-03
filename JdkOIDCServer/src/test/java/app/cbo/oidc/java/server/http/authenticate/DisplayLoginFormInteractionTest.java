package app.cbo.oidc.java.server.http.authenticate;

import app.cbo.oidc.java.server.TestHttpExchange;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DisplayLoginFormInteractionTest {

    @Test
    void nominal() throws IOException {
        final var ongoingAuthId = UUID.randomUUID().toString();
        var tested = new DisplayLoginFormInteraction(ongoingAuthId);

        var req = TestHttpExchange.simpleGet();
        tested.handle(req);

        assertThat(req.getResponseCode())
                .isEqualTo(200);

        var html = new String(req.getResponseBodyBytes(), StandardCharsets.UTF_8);

        assertThat(html)
                .isNotEmpty()
                .contains("""
                        <input name="ongoing" type="hidden" value="%s"/>
                        """.formatted(ongoingAuthId));


    }


}