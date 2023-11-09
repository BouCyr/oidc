package app.cbo.oidc.java.server.http.consent;

import app.cbo.oidc.java.server.TestHttpExchange;
import app.cbo.oidc.java.server.backends.sessions.Sessions;
import app.cbo.oidc.java.server.credentials.AuthenticationMode;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.http.AuthErrorInteraction;
import app.cbo.oidc.java.server.http.authorize.AuthorizeParams;
import app.cbo.oidc.java.server.oidc.OIDCDisplayValues;
import app.cbo.oidc.java.server.oidc.OIDCPromptValues;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class ConsentHandlerTest {

    @Test
    void path() {
        assertThat(new ConsentHandler(null, null, null).path())
                .isEqualTo(ConsentHandler.CONSENT_ENDPOINT);
    }

    @Test
    void displayForm() throws IOException, URISyntaxException {

        final var params = testAuthParams();
        var tested = new ConsentHandler(
                key -> Optional.of(params),
                (maybeSession, someParams) -> {
                    assertThat(maybeSession).isPresent();
                    assertThat(someParams).isNotNull();
                    assertThat(someParams.ongoing().nonce().get()).isEqualTo(params.nonce().get());

                    return exchange -> exchange.sendResponseHeaders(200, 0);
                },
                id -> Optional.of(new Session(UserId.of("user"), EnumSet.of(AuthenticationMode.DECLARATIVE)))
        );

        var req = new TestHttpExchange("GET", new URI("http://oidc.cbo.app/consent?ongoing=toto"), new ByteArrayInputStream(new byte[]{}));
        req.getRequestHeaders().add("Cookie", Sessions.SESSION_ID_COOKIE_NAME + "=sessionId");

        tested.handle(req);
    }

    @Test
    void handleFromSubmit() throws IOException, URISyntaxException {

        final var params = testAuthParams();
        var tested = new ConsentHandler(
                key -> Optional.of(params),
                (maybeSession, someParams) -> {
                    assertThat(maybeSession).isPresent();
                    assertThat(someParams).isNotNull();
                    assertThat(someParams.ongoing().nonce().get()).isEqualTo(params.nonce().get());

                    return exchange -> exchange.sendResponseHeaders(200, 0);
                },
                id -> Optional.of(new Session(UserId.of("user"), EnumSet.of(AuthenticationMode.DECLARATIVE)))
        );

        var req = new TestHttpExchange("GET", new URI("http://oidc.cbo.app/consent?ongoing=toto&" + ConsentParams.BACK + "=back"), new ByteArrayInputStream(new byte[]{}));
        req.getRequestHeaders().add("Cookie", Sessions.SESSION_ID_COOKIE_NAME + "=sessionId");

        tested.handle(req);
    }

    @Test
    void no_sessions() throws IOException, URISyntaxException {

        final var params = testAuthParams();
        var tested = new ConsentHandler(
                key -> Optional.of(params),
                (maybeSession, someParams) -> {
                    assertThat(maybeSession).isEmpty();
                    return exchange -> exchange.sendResponseHeaders(200, 0);
                },
                id -> Optional.empty() // !!!
        );

        var req = new TestHttpExchange("GET", new URI("http://oidc.cbo.app/consent?ongoing=toto"), new ByteArrayInputStream(new byte[]{}));
        req.getRequestHeaders().add("Cookie", Sessions.SESSION_ID_COOKIE_NAME + "=sessionId");

        tested.handle(req);
    }

    @Test
    void endpoint_managedError() throws IOException, URISyntaxException {

        final var params = testAuthParams();
        var tested = new ConsentHandler(
                key -> Optional.of(params),
                // !!!!!
                (maybeSession, someParams) -> {
                    throw new AuthErrorInteraction(AuthErrorInteraction.Code.invalid_grant, "this should be returned");
                },
                id -> Optional.of(new Session(UserId.of("user"), EnumSet.of(AuthenticationMode.DECLARATIVE)))
        );

        var req = new TestHttpExchange("GET", new URI("http://oidc.cbo.app/consent?ongoing=toto"), new ByteArrayInputStream(new byte[]{}));
        req.getRequestHeaders().add("Cookie", Sessions.SESSION_ID_COOKIE_NAME + "=sessionId");

        tested.handle(req);
        assertThat(req.getResponseCode()).isEqualTo(500);
        var body = new String(req.getResponseBodyBytes(), StandardCharsets.UTF_8);
        assertThat(body)
                .contains(AuthErrorInteraction.Code.invalid_grant.name())
                .contains("this should be returned");

    }

    @Test
    void endpoint_runtime() throws IOException, URISyntaxException {

        final var params = testAuthParams();
        var tested = new ConsentHandler(
                key -> Optional.of(params),
                // !!!!!
                (maybeSession, someParams) -> {
                    throw new RuntimeException("this could be returned");
                },
                id -> Optional.of(new Session(UserId.of("user"), EnumSet.of(AuthenticationMode.DECLARATIVE)))
        );

        var req = new TestHttpExchange("GET", new URI("http://oidc.cbo.app/consent?ongoing=toto"), new ByteArrayInputStream(new byte[]{}));
        req.getRequestHeaders().add("Cookie", Sessions.SESSION_ID_COOKIE_NAME + "=sessionId");

        tested.handle(req);
        assertThat(req.getResponseCode()).isEqualTo(500);
        var body = new String(req.getResponseBodyBytes(), StandardCharsets.UTF_8);
        assertThat(body)
                .contains(AuthErrorInteraction.Code.server_error.name())
                .doesNotContain("this could be returned"); //exception msg should not be exposed

    }

    private AuthorizeParams testAuthParams() {
        return new AuthorizeParams(
                List.of("scope1", "scope2"),
                List.of("RESPONSE_TYPE"),
                Optional.of("CLIENT"),
                Optional.of("http://clinet.cbo.app"),
                Optional.of("STATE"),
                Optional.of("RESPONSE_MODE"),
                Optional.of(UUID.randomUUID().toString()),
                Optional.of(OIDCDisplayValues.PAGE),
                List.of(OIDCPromptValues.LOGIN, OIDCPromptValues.CONSENT),
                Optional.empty(),
                List.of("FR-fr"),
                Optional.empty(),
                Optional.empty(),
                Collections.emptyList()
        );
    }
}