package app.cbo.oidc.java.server.http.token;

import app.cbo.oidc.java.server.TestHttpExchange;
import app.cbo.oidc.java.server.http.userinfo.ForbiddenResponse;
import app.cbo.oidc.java.server.utils.HttpCode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class TokenHandlerTest {

    @Test
    void path() {

        assertThat(new TokenHandler(null).path()).isEqualTo(TokenHandler.TOKEN_ENDPOINT);
    }

    @Test
    void no_data() throws IOException {

        TokenEndpoint mockEndpoint = (p, a, s) -> exchange -> {
            exchange.setAttribute("params", p);
            exchange.setAttribute("authClientId", a);
            exchange.setAttribute("clientSecret", s);

            exchange.sendResponseHeaders(200, 0);
        };
        var tested = new TokenHandler(mockEndpoint);
        var req = TestHttpExchange.simpleGet();

        tested.handle(req);

        assertThat(req.getAttribute("params")).isNotNull()
                .isInstanceOf(TokenParams.class);
        assertThat(req.getAttribute("authClientId"))
                .isNull();
        assertThat(req.getAttribute("clientSecret"))
                .isNull();
    }

    @Test
    void authent() throws IOException {

        TokenEndpoint mockEndpoint = (p, a, s) -> exchange -> {
            exchange.setAttribute("params", p);
            exchange.setAttribute("authClientId", a);
            exchange.setAttribute("clientSecret", s);

            exchange.sendResponseHeaders(200, 0);
        };
        var tested = new TokenHandler(mockEndpoint);
        var req = TestHttpExchange.simpleGet();

        var creds = Base64.getEncoder().encodeToString("CLIENT_ID:SECRET".getBytes(StandardCharsets.UTF_8));
        req.getRequestHeaders().add("Authorization", "Basic " + creds);
        tested.handle(req);

        assertThat(req.getAttribute("params")).isNotNull()
                .isInstanceOf(TokenParams.class);

        assertThat(req.getAttribute("authClientId"))
                .isNotNull();
        assertThat(req.getAttribute("authClientId").toString())
                .isEqualTo("CLIENT_ID");

        assertThat(req.getAttribute("clientSecret"))
                .isNotNull();
        assertThat(req.getAttribute("clientSecret").toString())
                .isEqualTo("SECRET");
    }

    @Test
    void receive_jsonException() throws IOException {
        TokenEndpoint mockEndpoint = (p, a, s) -> {
            throw new JsonError("invalid json");
        };
        var tested = new TokenHandler(mockEndpoint);
        var req = TestHttpExchange.simpleGet();

        var creds = Base64.getEncoder().encodeToString("CLIENT_ID:SECRET".getBytes(StandardCharsets.UTF_8));
        req.getRequestHeaders().add("Authorization", "Basic " + creds);
        tested.handle(req);

        assertThat(req.getResponseCode()).isEqualTo(400);

    }

    @Test
    void receive_ForbiddenResponse() throws IOException {
        TokenEndpoint mockEndpoint = (p, a, s) -> {
            throw new ForbiddenResponse(HttpCode.BAD_REQUEST, ForbiddenResponse.InternalReason.WRONG_TYPE, "invalid json");

        };
        var tested = new TokenHandler(mockEndpoint);
        var req = TestHttpExchange.simpleGet();

        var creds = Base64.getEncoder().encodeToString("CLIENT_ID:SECRET".getBytes(StandardCharsets.UTF_8));
        req.getRequestHeaders().add("Authorization", "Basic " + creds);
        tested.handle(req);

        assertThat(req.getResponseCode()).isEqualTo(400);
    }

    @Test
    void receive_randomException() throws IOException {
        TokenEndpoint mockEndpoint = (p, a, s) -> {
            throw new IndexOutOfBoundsException("random runtime exception");

        };
        var tested = new TokenHandler(mockEndpoint);
        var req = TestHttpExchange.simpleGet();

        var creds = Base64.getEncoder().encodeToString("CLIENT_ID:SECRET".getBytes(StandardCharsets.UTF_8));
        req.getRequestHeaders().add("Authorization", "Basic " + creds);
        tested.handle(req);

        assertThat(req.getResponseCode()).isEqualTo(500);
    }


}