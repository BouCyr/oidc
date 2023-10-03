package app.cbo.oidc.java.server.http.authenticate;

import app.cbo.oidc.java.server.TestHttpExchange;
import app.cbo.oidc.java.server.http.AuthErrorInteraction;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthenticateHandlerTest {

    @Test
    void path() {
        AuthenticateHandler tested = new AuthenticateHandler(p -> x -> {
        });

        assertThat(tested.path()).isEqualTo(AuthenticateHandler.AUTHENTICATE_ENDPOINT);
    }

    @Test
    void invalid_args() throws IOException, URISyntaxException {
        AuthenticateHandler tested = new AuthenticateHandler(p -> x -> x.sendResponseHeaders(200, 0));
        TestHttpExchange req = new TestHttpExchange("not_an_http_verb", new URI("http://oidc.cbo.app?test=foo"), new ByteArrayInputStream(new byte[]{}));

        tested.handle(req);

        assertThat(req.getResponseCode()).isEqualTo(500);
        var msg = new String(req.getResponseBodyBytes(), StandardCharsets.UTF_8);
        assertThat(msg)
                .contains("invalid_request")
                .contains("Invalid HTTP method");
    }


    @Test
    void nominal() throws URISyntaxException, IOException {
        AuthenticateHandler tested = new AuthenticateHandler(p -> x -> x.sendResponseHeaders(299, 0));
        TestHttpExchange req = new TestHttpExchange("GET", new URI("http://oidc.cbo.app"), new ByteArrayInputStream(new byte[]{}));

        tested.handle(req);

        //check the call was delegated to our mock endpoint
        assertThat(req.getResponseCode()).isEqualTo(299);
    }

    @Test
    void managed_exception_wo_redirect() throws URISyntaxException, IOException {
        var msg = UUID.randomUUID().toString();
        AuthenticateHandler tested = new AuthenticateHandler(p -> {
            throw new AuthErrorInteraction(AuthErrorInteraction.Code.access_denied, msg);
        });
        TestHttpExchange req = new TestHttpExchange("GET", new URI("http://oidc.cbo.app"), new ByteArrayInputStream(new byte[]{}));

        tested.handle(req);

        assertThat(req.getResponseCode()).isEqualTo(500);
        var errorMsg = new String(req.getResponseBodyBytes(), StandardCharsets.UTF_8);
        assertThat(errorMsg)
                .isNotEmpty()
                .contains(msg);

    }

    @Test
    void unexpected_exception_wo_redirect() throws URISyntaxException, IOException {
        AuthenticateHandler tested = new AuthenticateHandler(p -> {
            throw new IllegalArgumentException();
        });
        TestHttpExchange req = new TestHttpExchange("GET", new URI("http://oidc.cbo.app"), new ByteArrayInputStream(new byte[]{}));

        tested.handle(req);

        assertThat(req.getResponseCode()).isEqualTo(500);
        var errorMsg = new String(req.getResponseBodyBytes(), StandardCharsets.UTF_8);
        assertThat(errorMsg)
                .isNotEmpty();

    }

    @Test
    void managed_exception_with_redirect() throws URISyntaxException, IOException {
        var msg = UUID.randomUUID().toString();

        AuthenticateHandler tested = new AuthenticateHandler(p -> {
            throw new AuthErrorInteraction(AuthErrorInteraction.Code.access_denied, msg, "http://client.cbo.app", "STATE");
        });
        TestHttpExchange req = new TestHttpExchange("GET", new URI("http://oidc.cbo.app?redirect_uri=http://client.cbo.app"), new ByteArrayInputStream(new byte[]{}));


        tested.handle(req);

        assertThat(req.getResponseCode()).isEqualTo(302);
        assertThat(req.getResponseHeaders())
                .containsKey("location");
        assertThat(req.getResponseHeaders().get("location"))
                .isNotEmpty()
                .hasSize(1);
        assertThat(req.getResponseHeaders().get("location").get(0))
                .isNotEmpty()
                .startsWith("http://client.cbo.app")
                .contains("state=STATE")
                .contains("error=" + AuthErrorInteraction.Code.access_denied.name())
                .contains(msg);


    }

}