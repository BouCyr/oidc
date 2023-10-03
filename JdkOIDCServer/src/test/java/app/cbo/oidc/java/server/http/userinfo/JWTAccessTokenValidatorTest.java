package app.cbo.oidc.java.server.http.userinfo;

import app.cbo.oidc.java.server.backends.keys.MemKeySet;
import app.cbo.oidc.java.server.jwt.JWA;
import app.cbo.oidc.java.server.jwt.JWS;
import app.cbo.oidc.java.server.oidc.Issuer;
import app.cbo.oidc.java.server.oidc.tokens.AccessOrRefreshToken;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JWTAccessTokenValidatorTest {

    @Test
    void validateAccessToken() throws ForbiddenResponse {

        var keyset = new MemKeySet();
        var tested = new JWTAccessTokenValidator(Issuer.of("http://oidc.cbo.app"), keyset);

        var clock = Clock.systemUTC();
        var jwtAccessToken = new AccessOrRefreshToken(
                "http://oidc.cbo.app",
                AccessOrRefreshToken.TYPE_ACCESS,
                "userID",
                Instant.now(clock).plus(Duration.ofMinutes(5L)).getEpochSecond(),
                List.of("scope1", "scope2"));

        var signed = JWS.jwsWrap(JWA.RS256, jwtAccessToken, keyset.current(), keyset.privateKey(keyset.current()).get());
        var decoded = tested.validateAccessToken(signed);

        assertThat(decoded)
                .isNotNull();
        assertThat(decoded.sub().getUserId())
                .isEqualTo("userID");
        assertThat(decoded.scopes())
                .containsExactlyInAnyOrder("scope1", "scope2");

    }

    @Test
    void invalid_jwt() throws ForbiddenResponse {

        var keyset = new MemKeySet();
        var tested = new JWTAccessTokenValidator(Issuer.of("http://oidc.cbo.app"), keyset);

        assertThatThrownBy(() -> tested.validateAccessToken("gfgfd.gfdgd"))
                .isInstanceOf(ForbiddenResponse.class);
    }

    @Test
    void wrong_typ() throws ForbiddenResponse {

        var keyset = new MemKeySet();
        var tested = new JWTAccessTokenValidator(Issuer.of("http://oidc.cbo.app"), keyset);

        var clock = Clock.systemUTC();
        var jwtAccessToken = new AccessOrRefreshToken(
                "http://oidc.cbo.app",
                "anytthins", //not the right typ !
                "userID",
                Instant.now(clock).plus(Duration.ofMinutes(55L)).getEpochSecond(),
                List.of("scope1", "scope2"));

        var signed = JWS.jwsWrap(JWA.RS256, jwtAccessToken, keyset.current(), keyset.privateKey(keyset.current()).get());
        assertThatThrownBy(() -> tested.validateAccessToken(signed))
                .isInstanceOf(ForbiddenResponse.class);

    }

    @Test
    void expired_jwt() throws ForbiddenResponse {

        var keyset = new MemKeySet();
        var tested = new JWTAccessTokenValidator(Issuer.of("http://oidc.cbo.app"), keyset);

        var clock = Clock.systemUTC();
        var jwtAccessToken = new AccessOrRefreshToken(
                "http://oidc.cbo.app",
                AccessOrRefreshToken.TYPE_ACCESS,
                "userID",
                Instant.now(clock).minus(Duration.ofMinutes(55L)).getEpochSecond(),//!!!in the past
                List.of("scope1", "scope2"));

        var signed = JWS.jwsWrap(JWA.RS256, jwtAccessToken, keyset.current(), keyset.privateKey(keyset.current()).get());
        assertThatThrownBy(() -> tested.validateAccessToken(signed))
                .isInstanceOf(ForbiddenResponse.class);

    }

    @Test
    void wrong_issuer() throws ForbiddenResponse {

        var keyset = new MemKeySet();
        var tested = new JWTAccessTokenValidator(Issuer.of("http://oidc.cbo.app"), keyset);

        var clock = Clock.systemUTC();
        var jwtAccessToken = new AccessOrRefreshToken(
                "http://OTHER.cbo.app", //OTHER issuer !!!
                AccessOrRefreshToken.TYPE_ACCESS,
                "userID",
                Instant.now(clock).plus(Duration.ofMinutes(55L)).getEpochSecond(),
                List.of("scope1", "scope2"));

        var signed = JWS.jwsWrap(JWA.RS256, jwtAccessToken, keyset.current(), keyset.privateKey(keyset.current()).get());
        assertThatThrownBy(() -> tested.validateAccessToken(signed))
                .isInstanceOf(ForbiddenResponse.class);

    }

    @Test
    void wrong_sig() throws ForbiddenResponse {

        var keyset = new MemKeySet();
        var tested = new JWTAccessTokenValidator(Issuer.of("http://oidc.cbo.app"), keyset);

        var clock = Clock.systemUTC();
        var jwtAccessToken = new AccessOrRefreshToken(
                "http://oidc.cbo.app",
                AccessOrRefreshToken.TYPE_ACCESS,
                "userID",
                Instant.now(clock).plus(Duration.ofMinutes(55L)).getEpochSecond(),
                List.of("scope1", "scope2"));

        var signed = JWS.jwsWrap(JWA.RS256, jwtAccessToken, keyset.current(), keyset.privateKey(keyset.current()).get());
        var parts = signed.split("\\.");
        var wrong_signed = parts[0] + "." + parts[1] + "." + "foobar";
        assertThatThrownBy(() -> tested.validateAccessToken(wrong_signed))
                .isInstanceOf(ForbiddenResponse.class);

    }
}