package app.cbo.oidc.java.server.http.token;

import app.cbo.oidc.java.server.TestHttpExchange;
import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.http.userinfo.ForbiddenResponse;
import app.cbo.oidc.java.server.utils.HttpCode;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void clientIdMismatch_throwsJsonError() throws Exception {
        // Mock TokenEndpoint (not strictly necessary for this specific error, but good practice)
        TokenEndpoint mockTokenEndpoint = mock(TokenEndpoint.class);

        // Instantiate TokenHandler with the mocked endpoint
        TokenHandler tested = new TokenHandler(mockTokenEndpoint);

        // Mock HttpExchange
        HttpExchange mockExchange = mock(HttpExchange.class);
        Headers requestHeaders = new Headers();
        String authClientId = "auth_client_id";
        String credentials = Base64.getEncoder().encodeToString((authClientId + ":client_secret").getBytes(StandardCharsets.UTF_8));
        requestHeaders.add("Authorization", "Basic " + credentials);

        String bodyClientId = "body_client_id";
        String requestBody = "client_id=" + bodyClientId + "&grant_type=authorization_code&code=some_code";
        ByteArrayInputStream requestBodyStream = new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8));

        when(mockExchange.getRequestMethod()).thenReturn("POST");
        when(mockExchange.getRequestURI()).thenReturn(new URI(TokenHandler.TOKEN_ENDPOINT));
        when(mockExchange.getRequestHeaders()).thenReturn(requestHeaders);
        when(mockExchange.getRequestBody()).thenReturn(requestBodyStream);
        when(mockExchange.getResponseBody()).thenReturn(mock(OutputStream.class)); //needed for the handle method


        // Assert that JsonError is thrown and check its properties
        JsonError thrownException = assertThrows(JsonError.class, () -> tested.handleInternal(mockExchange));

        assertEquals("invalid_request", thrownException.getError());
        assertEquals("client_id in request body does not match authenticated client", thrownException.getErrorDescription());
    }
}