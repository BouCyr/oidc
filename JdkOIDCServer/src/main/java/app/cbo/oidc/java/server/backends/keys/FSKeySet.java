package app.cbo.oidc.java.server.backends.keys;

import app.cbo.oidc.java.server.backends.filesystem.FileSpecification;
import app.cbo.oidc.java.server.backends.filesystem.FileSpecifications;
import app.cbo.oidc.java.server.backends.filesystem.FileStorage;
import app.cbo.oidc.java.server.datastored.KeyId;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jwt.JWK;
import app.cbo.oidc.java.server.jwt.JWKSet;
import app.cbo.oidc.java.server.scan.Injectable;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@Injectable
public record FSKeySet(@NotNull FileStorage userDataFileStorage) implements KeySet {
    public static final FileSpecification KeyFile = FileSpecifications.in("keys").fileName("keySet.txt");

    private final static Logger LOGGER = Logger.getLogger(FSKeySet.class.getCanonicalName());

    public FSKeySet(@NotNull FileStorage userDataFileStorage) {
        this.userDataFileStorage = userDataFileStorage;

        Optional<BufferedReader> keyStore;
        try {
            keyStore = this.userDataFileStorage.reader(KeyFile);
        } catch (IOException e) {
            throw new RuntimeException("Error while locate keystore file");
        }
        if (keyStore.isEmpty()) {
            this.init();
        }
    }

    private static KeyId findCurrent(List<KeyPair> stored) {
        return stored.stream()
                .filter(KeyPair::current)
                .map(KeyPair::keyId)
                .findAny().orElseThrow(() -> new RuntimeException("No current key found"));
    }

    private void init() {
        KeyPairGenerator kpg;
        try {
            KeyPair newKP = newCurrent();
            this.writeKeys(newKP);

        } catch (NoSuchAlgorithmException e) {
            LOGGER.severe("NoSuchAlgorithmException when building keyset : " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private KeyPair newCurrent() throws NoSuchAlgorithmException {

        var start = System.nanoTime();
        KeyPairGenerator kpg;
        kpg = KeyPairGenerator.getInstance("RSA");
        //rfc7518 : A key of size 2048 bits or larger MUST be used with these algorithms.
        kpg.initialize(2048);
        var kp = kpg.generateKeyPair();
        var dur = Duration.ofNanos(System.nanoTime() - start).toMillis();
        LOGGER.info(STR."Generated new keypair in \{dur} ms;");
        //randomize the kid, so we do not reuse a kid (if we did, a client could store the 'old' key value in some cache)
        return new KeyPair(true, KeyId.of(UUID.randomUUID().toString()), kp.getPrivate(), kp.getPublic());
    }

    @NotNull
    private void writeKeys(@NotNull KeyPair... keyPairs) {
        try (var writer = this.userDataFileStorage.writer(KeyFile)) {
            for (var kp : keyPairs) {
                writer.write(this.toLine(kp));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @NotNull
    private String toLine(@NotNull KeyPair kp) {
        return kp.keyId().getKeyId()
                + ";"
                + (kp.current() ? "1" : "0")
                + ";"
                + Base64.getEncoder().encodeToString(kp.privateKey().getEncoded())
                + ";"
                + Base64.getEncoder().encodeToString(kp.publicKey().getEncoded());
    }

    @NotNull
    private KeyPair fromLine(KeyFactory keyFactory, String s) {
        try {
            String[] parts = s.split(";");
            String keyId = parts[0];
            boolean current = "1".equals(parts[1]);

            var privateKeyBytes = Base64.getDecoder().decode(parts[2]);
            EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            PrivateKey pk = keyFactory.generatePrivate(privateKeySpec);

            var publicKeyBytes = Base64.getDecoder().decode(parts[3]);
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            PublicKey pub = keyFactory.generatePublic(publicKeySpec);

            return new KeyPair(current, KeyId.of(keyId), pk, pub);
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse keyset line", e);
        }
    }

    @NotNull
    @Override
    public JWKSet asJWKSet() {

        return new JWKSet(
                this.readKeySet().stream()
                        .map(kp -> JWK.rsaPublicKey(kp.keyId().getKeyId(), (RSAPublicKey) kp.publicKey()))
                        .toList()
        );
    }

    @NotNull
    private List<KeyPair> readKeySet() {
        final KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }


        try (var reader = this.userDataFileStorage().reader(KeyFile)
                .orElseThrow(() -> new RuntimeException("Unable to locate keyset"))) {
            return reader
                    .lines()
                    .map(line -> this.fromLine(keyFactory, line))
                    .toList();
        } catch (IOException e) {
            throw new RuntimeException("Unable to read keyset file", e);
        }
    }

    @NotNull
    public KeyId rotate() {
        List<KeyPair> next = new ArrayList<>();
        this.readKeySet()
                .forEach(prev -> next.add(
                        new KeyPair(false, prev.keyId(), prev.privateKey(), prev.publicKey())
                ));
        try {
            next.add(this.newCurrent());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        this.writeKeys(next.toArray(new KeyPair[0]));
        return findCurrent(next);


    }

    @NotNull
    @Override
    public KeyId current() {
        var stored = this.readKeySet();
        return findCurrent(stored);
    }

    @NotNull
    @Override
    public Optional<PrivateKey> privateKey(@NotNull KeyId keyId) {
        return this.readKeySet().stream()
                .filter(kp -> keyId.getKeyId().equals(kp.keyId().getKeyId()))
                .map(KeyPair::privateKey)
                .findAny();
    }

    @NotNull
    @Override
    public Optional<PublicKey> publicKey(@NotNull KeyId keyId) {
        return this.readKeySet().stream()
                .filter(kp -> keyId.getKeyId().equals(kp.keyId().getKeyId()))
                .map(KeyPair::publicKey)
                .findAny();
    }

    record KeyPair(boolean current, @NotNull KeyId keyId, @NotNull PrivateKey privateKey,
                   @NotNull PublicKey publicKey) {
    }
}
