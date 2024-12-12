package app.cbo.oidc.java.server.http.token;

import app.cbo.oidc.java.server.TestHttpExchange;
import app.cbo.oidc.java.server.http.token.endpoints.CodeToTokensEndpoint;
import app.cbo.oidc.java.server.http.token.endpoints.RefreshTokenToTokensEndpoint;
import app.cbo.oidc.java.server.http.token.params.TokenParams;
import app.cbo.oidc.java.server.http.userinfo.ForbiddenResponse;
import app.cbo.oidc.java.server.utils.HttpCode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class TokenHandlerTest {

    public static final CodeToTokensEndpoint MOCK_CODE_ENDPOINT = (p, a, s) -> exchange -> {
        exchange.setAttribute("params", p);
        exchange.setAttribute("authClientId", a);
        exchange.setAttribute("clientSecret", s);

        exchange.sendResponseHeaders(200, 0);
    };

    public static final RefreshTokenToTokensEndpoint MOCK_REFRESH_ENDPOINT = (p, a, s) -> exchange -> {
        exchange.setAttribute("params", p);
        exchange.setAttribute("authClientId", a);
        exchange.setAttribute("clientSecret", s);

        exchange.sendResponseHeaders(200, 0);
    };

    @Test
    void path() {

        assertThat(new TokenHandler(null, null).path()).isEqualTo(TokenHandler.TOKEN_ENDPOINT);
    }

    @Test
    void no_data() throws IOException {

        var tested = new TokenHandler(MOCK_CODE_ENDPOINT, MOCK_REFRESH_ENDPOINT);
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


        var tested = new TokenHandler(MOCK_CODE_ENDPOINT, MOCK_REFRESH_ENDPOINT);
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
        CodeToTokensEndpoint mockEndpoint = (p, a, s) -> new JsonError(JsonError.Cause.invalid_request);
        var tested = new TokenHandler(mockEndpoint, MOCK_REFRESH_ENDPOINT);
        var req = TestHttpExchange.simpleGet();

        var creds = Base64.getEncoder().encodeToString("CLIENT_ID:SECRET".getBytes(StandardCharsets.UTF_8));
        req.getRequestHeaders().add("Authorization", "Basic " + creds);
        tested.handle(req);

        assertThat(req.getResponseCode()).isEqualTo(400);

    }

    @Test
    void receive_ForbiddenResponse() throws IOException {
        CodeToTokensEndpoint mockEndpoint = (p, a, s) -> new ForbiddenResponse(HttpCode.BAD_REQUEST, ForbiddenResponse.InternalReason.WRONG_TYPE, "invalid json");
        var tested = new TokenHandler(mockEndpoint, MOCK_REFRESH_ENDPOINT);
        var req = TestHttpExchange.simpleGet();

        var creds = Base64.getEncoder().encodeToString("CLIENT_ID:SECRET".getBytes(StandardCharsets.UTF_8));
        req.getRequestHeaders().add("Authorization", "Basic " + creds);
        tested.handle(req);

        assertThat(req.getResponseCode()).isEqualTo(400);
    }

    @Test
    void receive_randomException() throws IOException {
        CodeToTokensEndpoint mockEndpoint = (p, a, s) -> {
            throw new IndexOutOfBoundsException("random runtime exception");

        };
        var tested = new TokenHandler(mockEndpoint, MOCK_REFRESH_ENDPOINT);
        var req = TestHttpExchange.simpleGet();

        var creds = Base64.getEncoder().encodeToString("CLIENT_ID:SECRET".getBytes(StandardCharsets.UTF_8));
        req.getRequestHeaders().add("Authorization", "Basic " + creds);
        tested.handle(req);

        assertThat(req.getResponseCode()).isEqualTo(500);
    }


}