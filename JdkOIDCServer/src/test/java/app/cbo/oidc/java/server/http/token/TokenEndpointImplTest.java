package app.cbo.oidc.java.server.http.token;

import app.cbo.oidc.java.server.TestHttpExchange;
import app.cbo.oidc.java.server.backends.keys.MemKeySet;
import app.cbo.oidc.java.server.credentials.AuthenticationMode;
import app.cbo.oidc.java.server.datastored.CodeData;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.datastored.user.User;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.http.userinfo.ForbiddenResponse;
import app.cbo.oidc.java.server.oidc.Issuer;
import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class TokenEndpointImplTest {

    @Test
    void nominal() throws ForbiddenResponse, JsonError, IOException {

        var tested = new TokenEndpointImpl(
                Issuer.of("http://oidc.cbo.app"),
                (x, y, z) -> java.util.Optional.of(new CodeData(UserId.of("userA"), SessionId.of("session"), List.of("s1", "s2", "s3"), "nonceZ")),
                x -> Optional.of(new User("userA", "", "")),
                id -> Optional.of(new Session(UserId.of("userA"), EnumSet.of(AuthenticationMode.DECLARATIVE))),
                new MemKeySet(),
                new IdTokenCustomizer.Noop(),
                (id, secret) -> id != null && id.equals(secret)
        );

        var interaction = tested.treatRequest(
                new TokenParams("authorization_code", "code", "http://client.cbo.app", "CLIENT"),
                "CLIENT",
                "CLIENT"
        );

        assertThat(interaction)
                .isInstanceOf(JsonResponse.class);
        var jsonInteraction = ((JsonResponse) interaction);
        var tokenResponse = jsonInteraction.response();

        assertThat(tokenResponse).isNotNull();

        assertThat(tokenResponse.access_token()).isNotEmpty();
        assertThat(tokenResponse.refresh_token()).isNotEmpty();
        assertThat(tokenResponse.id_token()).isNotEmpty();
        assertThat(tokenResponse.scope()).isNotBlank();
        assertThat(tokenResponse.scope().split(" ")).containsExactlyInAnyOrder("s1", "s2", "s3");


        var idToken = JWT.decode(tokenResponse.id_token());
        assertThat(idToken.getSubject()).isEqualTo("userA");
        assertThat(idToken.getIssuer()).isEqualTo("http://oidc.cbo.app");
        assertThat(idToken.getAudience()).containsExactlyInAnyOrder("CLIENT");

        // issued less than 5 seconds ago
        // [04/10/2023] 5 seconds seems A LOT, but remember we had to compute 3 different RSA256 sig.
        // 5s is more than enough, 1s could fail if the CPU is somehow busy (e.g. running Tus...)
        assertThat(idToken.getIssuedAt()).isCloseTo(new Date(), 5_000L);


        //play the interaction
        var req = TestHttpExchange.simpleGet();
        jsonInteraction.handle(req);

        assertThat(req.getResponseCode()).isEqualTo(200);
        assertThatNoException().isThrownBy(() -> new ObjectMapper().reader().readTree(req.getResponseBodyBytes()));
        assertThat(req.getResponseHeaders()).containsKey("Content-Type");
        assertThat(req.getResponseHeaders().get("Content-Type")).hasSize(1);
        assertThat(req.getResponseHeaders().get("Content-Type").getFirst()).isEqualTo("application/json");
    }

    @Test
    void invalid_client_credentials() throws ForbiddenResponse, JsonError, IOException {
        var tested = new TokenEndpointImpl(
                Issuer.of("http://oidc.cbo.app"),
                (x, y, z) -> java.util.Optional.of(new CodeData(UserId.of("userA"), SessionId.of("session"), List.of("s1", "s2", "s3"), "nonceZ")),
                x -> Optional.of(new User("userA", "", "")),
                id -> Optional.of(new Session(UserId.of("userA"), EnumSet.of(AuthenticationMode.DECLARATIVE))),
                new MemKeySet(),
                new IdTokenCustomizer.Noop(),
                (id, secret) -> id != null && id.equals(secret)
        );

        var interaction = tested.treatRequest(
                new TokenParams("authorization_code", "code", "http://client.cbo.app", "CLIENT"),
                "CLIENT",
                "wrong_wrong_wrong" //WRONG !!!
        );

        assertThat(interaction)
                .isInstanceOf(JsonError.class);
        var jsonError = (JsonError) interaction;
        Error error = new ObjectMapper().reader().readValue(jsonError.json(), Error.class);

        assertThat(error.error()).isEqualTo("access_denied");
    }

    @Test
    void no_redirect_uri() throws ForbiddenResponse, JsonError, IOException {
        var tested = new TokenEndpointImpl(
                Issuer.of("http://oidc.cbo.app"),
                (x, y, z) -> java.util.Optional.of(new CodeData(UserId.of("userA"), SessionId.of("session"), List.of("s1", "s2", "s3"), "nonceZ")),
                x -> Optional.of(new User("userA", "", "")),
                id -> Optional.of(new Session(UserId.of("userA"), EnumSet.of(AuthenticationMode.DECLARATIVE))),
                new MemKeySet(),
                new IdTokenCustomizer.Noop(),
                (id, secret) -> id != null && id.equals(secret)
        );

        var interaction = tested.treatRequest(
                new TokenParams("authorization_code", "code", "", "CLIENT"),
                "CLIENT",
                "CLIENT"
        );

        assertThat(interaction)
                .isInstanceOf(JsonError.class);
        var jsonError = (JsonError) interaction;
        Error error = new ObjectMapper().reader().readValue(jsonError.json(), Error.class);

        assertThat(error.error()).isEqualTo("redirecturi not present");
    }

    @Test
    void null_redirect_uri() throws ForbiddenResponse, JsonError, IOException {
        var tested = new TokenEndpointImpl(
                Issuer.of("http://oidc.cbo.app"),
                (x, y, z) -> java.util.Optional.of(new CodeData(UserId.of("userA"), SessionId.of("session"), List.of("s1", "s2", "s3"), "nonceZ")),
                x -> Optional.of(new User("userA", "", "")),
                id -> Optional.of(new Session(UserId.of("userA"), EnumSet.of(AuthenticationMode.DECLARATIVE))),
                new MemKeySet(),
                new IdTokenCustomizer.Noop(),
                (id, secret) -> id != null && id.equals(secret)
        );

        var interaction = tested.treatRequest(
                new TokenParams("authorization_code", "code", null, "CLIENT"),
                "CLIENT",
                "CLIENT"
        );

        assertThat(interaction)
                .isInstanceOf(JsonError.class);
        var jsonError = (JsonError) interaction;
        Error error = new ObjectMapper().reader().readValue(jsonError.json(), Error.class);

        assertThat(error.error()).isEqualTo("redirecturi not present");
    }

    @Test
    void no_grant_type() throws ForbiddenResponse, JsonError, IOException {
        var tested = new TokenEndpointImpl(
                Issuer.of("http://oidc.cbo.app"),
                (x, y, z) -> java.util.Optional.of(new CodeData(UserId.of("userA"), SessionId.of("session"), List.of("s1", "s2", "s3"), "nonceZ")),
                x -> Optional.of(new User("userA", "", "")),
                id -> Optional.of(new Session(UserId.of("userA"), EnumSet.of(AuthenticationMode.DECLARATIVE))),
                new MemKeySet(),
                new IdTokenCustomizer.Noop(),
                (id, secret) -> id != null && id.equals(secret)
        );

        var interaction = tested.treatRequest(
                new TokenParams("",//!!!!
                        "code", "http://client.cbo.app", "CLIENT"),
                "CLIENT",
                "CLIENT"
        );

        assertThat(interaction)
                .isInstanceOf(JsonError.class);
        var jsonError = (JsonError) interaction;
        Error error = new ObjectMapper().reader().readValue(jsonError.json(), Error.class);

        assertThat(error.error()).isEqualTo("grant type not present");
    }

    @Test
    void null_grant_type() throws ForbiddenResponse, JsonError, IOException {
        var tested = new TokenEndpointImpl(
                Issuer.of("http://oidc.cbo.app"),
                (x, y, z) -> java.util.Optional.of(new CodeData(UserId.of("userA"), SessionId.of("session"), List.of("s1", "s2", "s3"), "nonceZ")),
                x -> Optional.of(new User("userA", "", "")),
                id -> Optional.of(new Session(UserId.of("userA"), EnumSet.of(AuthenticationMode.DECLARATIVE))),
                new MemKeySet(),
                new IdTokenCustomizer.Noop(),
                (id, secret) -> id != null && id.equals(secret)
        );

        var interaction = tested.treatRequest(
                new TokenParams(null,//!!!!
                        "code", "http://client.cbo.app", "CLIENT"),
                "CLIENT",
                "CLIENT"
        );

        assertThat(interaction)
                .isInstanceOf(JsonError.class);
        var jsonError = (JsonError) interaction;
        Error error = new ObjectMapper().reader().readValue(jsonError.json(), Error.class);

        assertThat(error.error()).isEqualTo("grant type not present");
    }

    @Test
    void invalid_grant_type() throws ForbiddenResponse, JsonError, IOException {
        var tested = new TokenEndpointImpl(
                Issuer.of("http://oidc.cbo.app"),
                (x, y, z) -> java.util.Optional.of(new CodeData(UserId.of("userA"), SessionId.of("session"), List.of("s1", "s2", "s3"), "nonceZ")),
                x -> Optional.of(new User("userA", "", "")),
                id -> Optional.of(new Session(UserId.of("userA"), EnumSet.of(AuthenticationMode.DECLARATIVE))),
                new MemKeySet(),
                new IdTokenCustomizer.Noop(),
                (id, secret) -> id != null && id.equals(secret)
        );

        var interaction = tested.treatRequest(
                new TokenParams("INVALID",//!!!!
                        "code", "http://client.cbo.app", "CLIENT"),
                "CLIENT",
                "CLIENT"
        );

        assertThat(interaction)
                .isInstanceOf(JsonError.class);
        var jsonError = (JsonError) interaction;
        Error error = new ObjectMapper().reader().readValue(jsonError.json(), Error.class);

        assertThat(error.error()).isEqualTo("invalid grant type");
    }

    @Test
    void userNotFound() throws ForbiddenResponse, JsonError, IOException {
        var tested = new TokenEndpointImpl(
                Issuer.of("http://oidc.cbo.app"),
                (x, y, z) -> java.util.Optional.of(new CodeData(UserId.of("userA"), SessionId.of("session"), List.of("s1", "s2", "s3"), "nonceZ")),
                x -> Optional.empty(),
                id -> Optional.of(new Session(UserId.of("userA"), EnumSet.of(AuthenticationMode.DECLARATIVE))),
                new MemKeySet(),
                new IdTokenCustomizer.Noop(),
                (id, secret) -> id != null && id.equals(secret)
        );

        var interaction = tested.treatRequest(
                new TokenParams("authorization_code",
                        "code", "http://client.cbo.app", "CLIENT"),
                "CLIENT",
                "CLIENT"
        );

        assertThat(interaction)
                .isInstanceOf(JsonError.class);
        var jsonError = (JsonError) interaction;
        Error error = new ObjectMapper().reader().readValue(jsonError.json(), Error.class);

        assertThat(error.error()).isEqualTo("user_not_found");
    }

    @Test
    void codeNotFound() throws ForbiddenResponse, JsonError, IOException {
        var tested = new TokenEndpointImpl(
                Issuer.of("http://oidc.cbo.app"),
                (x, y, z) -> java.util.Optional.empty(),
                x -> Optional.of(new User("userA", "", "")),
                id -> Optional.of(new Session(UserId.of("userA"), EnumSet.of(AuthenticationMode.DECLARATIVE))),
                new MemKeySet(),
                new IdTokenCustomizer.Noop(),
                (id, secret) -> id != null && id.equals(secret)
        );

        var interaction = tested.treatRequest(
                new TokenParams("authorization_code",
                        "code", "http://client.cbo.app", "CLIENT"),
                "CLIENT",
                "CLIENT"
        );

        assertThat(interaction)
                .isInstanceOf(JsonError.class);
        var jsonError = (JsonError) interaction;
        Error error = new ObjectMapper().reader().readValue(jsonError.json(), Error.class);

        assertThat(error.error()).isEqualTo("access_denied");
    }

    @Test
    void sessionNotFound() throws ForbiddenResponse, JsonError, IOException {
        var tested = new TokenEndpointImpl(
                Issuer.of("http://oidc.cbo.app"),
                (x, y, z) -> java.util.Optional.of(new CodeData(UserId.of("userA"), SessionId.of("session"), List.of("s1", "s2", "s3"), "nonceZ")),
                x -> Optional.of(new User("userA", "", "")),
                id -> Optional.empty(),
                new MemKeySet(),
                new IdTokenCustomizer.Noop(),
                (id, secret) -> id != null && id.equals(secret)
        );

        var interaction = tested.treatRequest(
                new TokenParams("authorization_code",
                        "code", "http://client.cbo.app", "CLIENT"),
                "CLIENT",
                "CLIENT"
        );

        assertThat(interaction)
                .isInstanceOf(JsonError.class);
        var jsonError = (JsonError) interaction;
        Error error = new ObjectMapper().reader().readValue(jsonError.json(), Error.class);

        assertThat(error.error()).isEqualTo("session_not_found");
    }

    public record Error(String error) {
    }
}