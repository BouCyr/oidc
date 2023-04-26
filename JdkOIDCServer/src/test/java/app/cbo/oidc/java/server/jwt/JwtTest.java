package app.cbo.oidc.java.server.jwt;

import app.cbo.oidc.java.server.oidc.tokens.IdToken;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Test;

import java.security.InvalidKeyException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTest {

    private final RSAPrivateKey privateK;
    private final RSAPublicKey publicK;

    static IdToken testToken(String sub, String iss) {
        return new IdToken(sub,
                iss,
                Collections.emptyList(),
                5 * 60 + new Date().getTime() / 1000,
                new Date().getTime() / 1000,
                new Date().getTime() / 1000,
                Optional.of("nonce"),
                "NONE",
                Collections.emptyList(),
                Optional.empty(),
                new HashMap<>()
        );
    }


    JwtTest() throws NoSuchAlgorithmException {
        var kpg = KeyPairGenerator.getInstance("RSA");
        var kp = kpg.generateKeyPair();

        this.privateK = (RSAPrivateKey) kp.getPrivate();
        this.publicK = (RSAPublicKey) kp.getPublic();


        //for validation using jwt.io

        System.out.println("-----BEGIN PUBLIC KEY-----");
        System.out.println(Base64.getEncoder().encodeToString(this.publicK.getEncoded()));
        System.out.println("-----END PUBLIC KEY-----");
        System.out.println();
        System.out.println("-----BEGIN PRIVATE KEY-----");
        System.out.println(Base64.getEncoder().encodeToString(this.privateK.getEncoded()));
        System.out.println("-----END PRIVATE KEY-----");

    }

    @Test
    void wrongKey() throws NoSuchAlgorithmException {
        var ecdsaPriv = KeyPairGenerator.getInstance("EC").generateKeyPair().getPrivate();
        IdToken payload = testToken("cyrille", "http://auth0.com");
        assertThatThrownBy(() -> JWS.jwsWrap(JWA.RS256, payload, "kid", ecdsaPriv))
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseInstanceOf(InvalidKeyException.class);
    }

    @Test
    void none() {

        IdToken payload = testToken("cyrille", "http://auth0.com");

        String token = JWS.jwsWrap(JWA.NONE, payload, null, null);

        var decoded = com.auth0.jwt.JWT.decode(token);
        assertThat(decoded.getIssuer()).isEqualTo("http://auth0.com");
        assertThat(decoded.getSubject()).isEqualTo("cyrille");

        var verifier = com.auth0.jwt.JWT.require(Algorithm.none()).build();
        var verified = verifier.verify(token);

        assertThat(verified.getIssuer()).isEqualTo("http://auth0.com");
        assertThat(verified.getSubject()).isEqualTo("cyrille");

    }

    @Test
    void RSA256() {

        IdToken payload = testToken("cyrille", "http://auth0.com");
        String token = JWS.jwsWrap(JWA.RS256, payload, "kid", this.privateK);

        var decoded = com.auth0.jwt.JWT.decode(token);
        assertThat(decoded.getIssuer()).isEqualTo("http://auth0.com");
        assertThat(decoded.getSubject()).isEqualTo("cyrille");

        var verifier = com.auth0.jwt.JWT.require(Algorithm.RSA256(this.publicK, this.privateK)).build();
        var verified = verifier.verify(token);

        assertThat(verified.getIssuer()).isEqualTo("http://auth0.com");
        assertThat(verified.getSubject()).isEqualTo("cyrille");

    }


    @Test
    void external() {


        Algorithm algorithm = Algorithm.RSA256(this.publicK, this.privateK);
        String token = com.auth0.jwt.JWT.create()
                .withIssuer("auth0")
                .withKeyId("keyid")
                .withSubject("cyrille")
                .sign(algorithm);

        var decoded = com.auth0.jwt.JWT.decode(token);

        var verifier = com.auth0.jwt.JWT.require(Algorithm.RSA256(this.publicK, this.privateK)).build();

        var verified = verifier.verify(token);
        assertThat(verified.getIssuer()).isEqualTo("auth0");
        assertThat(verified.getSubject()).isEqualTo("cyrille");

    }

}