package app.cbo.oidc.java.server.backends.keys;

import app.cbo.oidc.java.server.datastored.KeyId;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jwt.JWK;
import app.cbo.oidc.java.server.jwt.JWKSet;
import app.cbo.oidc.java.server.scan.Injectable;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Injectable("mem")
public class MemKeySet implements KeySet {

    private final static Logger LOGGER = Logger.getLogger(MemKeySet.class.getCanonicalName());


    private KeyId currentKp;
    private final Map<String, KeyPair> pairs = new HashMap<>();

    public MemKeySet() {
        newCurrent();

    }

    private void newCurrent() {
        KeyPairGenerator kpg;
        try {
            var start = System.nanoTime();
            kpg = KeyPairGenerator.getInstance("RSA");
            //rfc7518 : A key of size 2048 bits or larger MUST be used with these algorithms.
            kpg.initialize(2048);
            var kp = kpg.generateKeyPair();
            var dur = Duration.ofNanos(System.nanoTime() - start).toMillis();

            //randomize the kid, so we do not reuse a kid (if we did, a client could store the 'old' key value in some cache)
            this.currentKp = KeyId.of(UUID.randomUUID().toString());
            this.pairs.put(currentKp.get(), kp);

        } catch (NoSuchAlgorithmException e) {
            LOGGER.severe("NoSuchAlgorithmException when building keyset : "+e.getMessage());
            throw new RuntimeException(e);
        }
    }


    @NotNull
    @Override
    public KeyId rotate() {
        this.newCurrent();
        return this.currentKp;
    }

    @Override
    @NotNull
    public JWKSet asJWKSet() {
        return new JWKSet(pairs.entrySet().stream()
                .filter(kp -> kp.getValue().getPublic() instanceof RSAPublicKey)
                .map(kp -> JWK.rsaPublicKey(kp.getKey(), (RSAPublicKey) kp.getValue().getPublic()))
                .toList());
    }

    @Override
    @NotNull
    public KeyId current() {
        return this.currentKp;
    }

    @Override
    @NotNull
    public Optional<PrivateKey> privateKey(@NotNull KeyId keyId) {
        return Optional.ofNullable(pairs.get(keyId.getKeyId())).map(KeyPair::getPrivate);
    }

    @Override
    @NotNull
    public Optional<PublicKey> publicKey(@NotNull KeyId keyId) {
        return Optional.ofNullable(pairs.get(keyId.getKeyId())).map(KeyPair::getPublic);
    }

}
