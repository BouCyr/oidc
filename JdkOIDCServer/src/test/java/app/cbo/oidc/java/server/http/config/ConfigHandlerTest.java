package app.cbo.oidc.java.server.http.config;

import app.cbo.oidc.java.server.TestHttpExchange;
import app.cbo.oidc.java.server.http.PathCustomizer;
import app.cbo.oidc.java.server.oidc.Issuer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigHandlerTest {

    ////FIXME [a118608][13/03/2024] 
//    @Test
//    void path() {
//
//        var tested = new ConfigHandler(
//                new PathCustomizer(),
//                null,
//                null,
//                null,
//                null,
//                null,
//                null);
//
//        Assertions.assertThat(tested.path()).isEqualTo(ConfigHandler.CONFIG_ENDPOINT);
//    }

    @Test
    void handle() throws IOException {
        var tested = new ConfigHandler(
                new PathCustomizer.Noop(),
                Issuer.of("http://oidc.cbo.app"),
                () -> "http://oidc.cbo.app/auth",
                () -> "http://oidc.cbo.app/token",
                () -> "http://oidc.cbo.app/userinfo",
//                () -> "http://oidc.cbo.app/logout",
                () -> "http://oidc.cbo.app/jwks");


        var req = TestHttpExchange.simpleGet();

        tested.handle(req);

        assertThat(req.getResponseCode()).isEqualTo(200);

        String json = new String(req.getResponseBodyBytes(), StandardCharsets.UTF_8);


        assertThat(req.getResponseHeaders())
                .containsKey("content-type");
        assertThat(req.getResponseHeaders().get("content-type"))
                .hasSize(1);
        assertThat(req.getResponseHeaders().get("content-type").get(0))
                .isEqualTo("application/json");

        var conf = new ObjectMapper().reader().readTree(json);


        assertThat(conf).isNotNull();

        checkNode(conf, "issuer", "http://oidc.cbo.app");
        checkNode(conf, "authorization_endpoint", "http://oidc.cbo.app/auth");
        checkNode(conf, "token_endpoint", "http://oidc.cbo.app/token");
        checkNode(conf, "userinfo_endpoint", "http://oidc.cbo.app/userinfo");
        checkNode(conf, "end_session_endpoint", "http://oidc.cbo.app/logout");
        checkNode(conf, "jwks_uri", "http://oidc.cbo.app/jwks");
    }

    private void checkNode(JsonNode conf, String key, String expectedContent) {
        assertThat(conf.get(key))
                .isNotNull()
                .isInstanceOf(TextNode.class);

        var content = conf.get(key).asText();
        assertThat(content).isEqualTo(expectedContent);
    }
}