package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.datastored.KeyId;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jwt.JWK;
import app.cbo.oidc.java.server.jwt.JWKSet;

import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public class KeySet {

    private final static Logger LOGGER = Logger.getLogger(KeySet.class.getCanonicalName());


    private final KeyId currentKp;
    private final Map<String, KeyPair> pairs = new HashMap<>();

    //TODO [28/04/2023] store on disk (java keystore ?)
    public KeySet() {
        KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
            //rfc7518 : A key of size 2048 bits or larger MUST be used with these algorithms.
            kpg.initialize(2048);
            var kp = kpg.generateKeyPair();


            //randomize the kid, so we do not reuse a kid (if we did, a client could store the 'old' key value in some cache)
            this.currentKp = KeyId.of(UUID.randomUUID().toString());
            this.pairs.put(currentKp.get(), kp);

        } catch (NoSuchAlgorithmException e) {
            LOGGER.severe("NoSuchAlgorithmException when building keyset : " + e.getMessage());
            throw new RuntimeException(e);
        }


    }

    @NotNull
    public JWKSet asJWKSet() {
        return new JWKSet(pairs.entrySet().stream()
                .filter(kp -> kp.getValue().getPublic() instanceof RSAPublicKey)
                .map(kp -> JWK.rsaPublicKey(kp.getKey(), (RSAPublicKey) kp.getValue().getPublic()))
                .toList());
    }

    @NotNull
    public KeyId current() {
        return this.currentKp;
    }

    @NotNull
    public Optional<PrivateKey> privateKey(@NotNull KeyId keyId) {
        return Optional.ofNullable(pairs.get(keyId.getKeyId())).map(KeyPair::getPrivate);
    }

    @NotNull
    public Optional<PublicKey> publicKey(@NotNull KeyId keyId) {
        return Optional.ofNullable(pairs.get(keyId.getKeyId())).map(KeyPair::getPublic);
    }

}
