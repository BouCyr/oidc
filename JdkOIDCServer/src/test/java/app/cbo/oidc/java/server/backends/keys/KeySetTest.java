package app.cbo.oidc.java.server.backends.keys;

import app.cbo.oidc.java.server.jwt.JWA;
import org.assertj.core.api.Assertions;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KeySetTest {


    void nominal(KeySet keyset) {

        var firstCurrent = keyset.current();

        var priv = keyset.privateKey(keyset.current());
        var pub = keyset.publicKey(keyset.current());

        assertThat(priv).isPresent();
        assertThat(pub).isPresent();

        var payloadOne = UUID.randomUUID().toString();
        var payloadTwo = UUID.randomUUID().toString();

        byte[] sigOne, sigTwo;
        try {
            sigOne = sign(priv.get(), payloadOne);
            sigTwo = sign(priv.get(), payloadTwo);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace(System.out);
            Assertions.fail("Signature failed");
            return;
        }

        try {
            boolean cool = verify(pub.get(), payloadOne, sigOne);
            assertThat(cool)
                    .isTrue();
        } catch (Exception e) {
            Assertions.fail("Verification failed");
            return;
        }

        keyset.rotate();

        var newCurrent = keyset.current();
        assertThat(newCurrent).isNotNull();
        assertThat(newCurrent.getKeyId()).isNotEqualTo(firstCurrent.getKeyId());

        var jwks = keyset.asJWKSet();

        assertThat(jwks.keys())
                .hasSize(2);
        assertThat(jwks.keys().stream().filter(kp -> kp.kid().equals(firstCurrent.getKeyId())))
                .hasSize(1);
        assertThat(jwks.keys().stream().filter(kp -> kp.kid().equals(newCurrent.getKeyId())))
                .hasSize(1);

    }

    private boolean verify(PublicKey pub, String payload, byte[] sigOne) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        Signature privateSignature = Signature.getInstance(JWA.RS256.javaName());
        privateSignature.initVerify(pub);
        privateSignature.update(payload.getBytes(StandardCharsets.UTF_8));
        var cool = privateSignature.verify(sigOne);
        return cool;
    }

    private byte[] sign(PrivateKey priv, String payload) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        byte[] sigOne;
        Signature privateSignature = Signature.getInstance(JWA.RS256.javaName());
        privateSignature.initSign(priv);
        privateSignature.update(payload.getBytes(StandardCharsets.UTF_8));
        sigOne = privateSignature.sign();
        return sigOne;
    }

}