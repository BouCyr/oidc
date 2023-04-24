package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.datastored.KeyId;
import app.cbo.oidc.java.server.jsr305.NotNull;

import java.security.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class KeySet {

    private static KeySet instance = null;
    private final KeyId currentKp;
    private final Map<String, KeyPair> pairs = new HashMap<>();

    private KeySet() {
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
            //rfc7518 : A key of size 2048 bits or larger MUST be used with these algorithms.
            kpg.initialize(2048);
            var kp = kpg.generateKeyPair();

            this.currentKp = KeyId.of("k1");
            this.pairs.put(currentKp.get(), kp);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); //TODO [14/04/2023]
        }


    }

    public static KeySet getInstance() {
        if (instance == null) {
            instance = new KeySet();
        }
        return instance;
    }

    public @NotNull
    KeyId current() {
        return this.currentKp;
    }

    public @NotNull
    Optional<PrivateKey> privateKey(@NotNull KeyId keyId) {
        return Optional.ofNullable(pairs.get(keyId.getKeyId())).map(KeyPair::getPrivate);
    }

    public @NotNull
    Optional<PublicKey> publicKey(@NotNull KeyId keyId) {
        return Optional.ofNullable(pairs.get(keyId.getKeyId())).map(KeyPair::getPublic);
    }
}
