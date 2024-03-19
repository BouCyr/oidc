package app.cbo.oidc.java.server.http.config;

import app.cbo.oidc.java.server.TestHttpExchange;
import app.cbo.oidc.java.server.backends.keys.MemKeySet;
import app.cbo.oidc.java.server.http.PathCustomizer;
import app.cbo.oidc.java.server.http.authorize.AuthorizeHandler;
import app.cbo.oidc.java.server.http.jwks.JWKSHandler;
import app.cbo.oidc.java.server.http.token.TokenHandler;
import app.cbo.oidc.java.server.http.userinfo.UserInfoHandler;
import app.cbo.oidc.java.server.oidc.Issuer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigHandlerTest {



    @Test
    void handle() throws IOException {
        var tested = new ConfigHandler(
                new PathCustomizer.Noop(),
                Issuer.of("http://oidc.cbo.app"),
                new AuthorizeHandler(null, null),
                new TokenHandler(null),
                new UserInfoHandler(null),
//                () -> "http://oidc.cbo.app/logout",
                new JWKSHandler(new MemKeySet()));


        var req = TestHttpExchange.simpleGet();

        tested.handle(req);

        assertThat(req.getResponseCode()).isEqualTo(200);

        String json = new String(req.getResponseBodyBytes(), StandardCharsets.UTF_8);


        assertThat(req.getResponseHeaders())
                .containsKey("content-type");
        assertThat(req.getResponseHeaders().get("content-type"))
                .hasSize(1);
        assertThat(req.getResponseHeaders().get("content-type").getFirst())
                .isEqualTo("application/json");

        var conf = new ObjectMapper().reader().readTree(json);


        assertThat(conf).isNotNull();

        checkNode(conf, "issuer", "http://oidc.cbo.app");
        checkNode(conf, "authorization_endpoint", "http://oidc.cbo.app/authorize");
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