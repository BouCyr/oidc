package app.cbo.oidc.java.server.http.staticcontent;

import app.cbo.oidc.java.server.TestHttpExchange;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaticResourceHandlerTest {

    @Test
    void path() {
        assertThat(new StaticResourceHandler().path())
                .isEqualTo(StaticResourceHandler.STATIC);

        assertThatThrownBy(Data::new)
                .isInstanceOf(IllegalAccessException.class);

    }

    @Test
    void css() throws URISyntaxException, IOException {

        var tested = new StaticResourceHandler();

        var req = new TestHttpExchange("GET", new URI("http://oidc.cbo.app/sc/clean.css"), new ByteArrayInputStream(new byte[]{}));

        tested.handle(req);

        assertThat(req.getResponseCode()).isEqualTo(200);

        String css = new String(req.getResponseBodyBytes(), StandardCharsets.UTF_8);

        assertThat(css).contains("h1");

        assertThat(req.getResponseHeaders())
                .containsKey("content-type");
        assertThat(req.getResponseHeaders().get("content-type"))
                .hasSize(1);
        assertThat(req.getResponseHeaders().get("content-type").get(0))
                .isEqualTo("text/css");


    }

    @Test
    void favico() throws URISyntaxException, IOException {

        var tested = new StaticResourceHandler();

        var req = new TestHttpExchange("GET", new URI("http://oidc.cbo.app/sc/fav.svg"), new ByteArrayInputStream(new byte[]{}));

        tested.handle(req);

        assertThat(req.getResponseCode()).isEqualTo(200);


        String css = new String(req.getResponseBodyBytes(), StandardCharsets.UTF_8);


        assertThat(css).contains("svg");

        assertThat(req.getResponseHeaders())
                .containsKey("content-type");
        assertThat(req.getResponseHeaders().get("content-type"))
                .hasSize(1);
        assertThat(req.getResponseHeaders().get("content-type").get(0))
                .isEqualTo("image/svg+xml");


    }

    @Test
    void not_found() throws URISyntaxException, IOException {

        var tested = new StaticResourceHandler();

        var req = new TestHttpExchange("GET",
                new URI("http://oidc.cbo.app/sc/" + UUID.randomUUID() + ".txt"),
                new ByteArrayInputStream(new byte[]{}));

        tested.handle(req);
        assertThat(req.getResponseCode()).isEqualTo(404);
    }

}