package app.cbo.oidc.java.server.backends.tokens;

import app.cbo.oidc.java.server.backends.keys.MemKeySet;
import app.cbo.oidc.java.server.credentials.AuthenticationMode;
import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.oidc.Issuer;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;


class JWTAccessTokensTest {

    @Test
    void gen() {
        var tested = new JWTAccessTokenGenerator(Issuer.of("myself"), new MemKeySet());

        var output = tested.generate(
                ClientId.of("someClinet"),
                new Session(UserId.of("user"), EnumSet.of(AuthenticationMode.USER_FOUND, AuthenticationMode.TOTP_OK)),
                "API");

        System.out.println(output);

    }

}