package app.cbo.oidc.java.server.http.jwks;

import app.cbo.oidc.java.server.TestHttpExchange;
import app.cbo.oidc.java.server.backends.keys.MemKeySet;
import app.cbo.oidc.java.server.utils.MimeType;
import com.nimbusds.jose.jwk.JWKSet;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JWKSHandlerTest {

    final JWKSHandler tested = new JWKSHandler(new MemKeySet());

    @Test
    void path() {

        var path = tested.path();
        assertThat(path)
                .isEqualTo(JWKSHandler.JWKS_ENDPOINT);

    }

    @Test
    void handle() throws Exception {

        var req = new TestHttpExchange("GET", new URI("http://oidc.cbo.app"), new ByteArrayInputStream(new byte[]{}));

        tested.handle(req);
        assertThat(req.getResponseCode())
                .isEqualTo(200);
        assertThat(req.getResponseHeaders())
                .containsKey("Content-Type");
        assertThat(req.getResponseHeaders().get("Content-Type"))
                .hasSize(1);
        assertThat(req.getResponseHeaders().get("Content-Type").getFirst())
                .isEqualTo(MimeType.JWKSET.mimeType());

        String response = new String(req.getResponseBodyBytes(), StandardCharsets.UTF_8);
        var parsed = JWKSet.parse(response);
        assertThat(parsed)
                .isNotNull();
        assertThat(parsed.getKeys())
                .isNotEmpty()
                .hasSize(1);


    }
}