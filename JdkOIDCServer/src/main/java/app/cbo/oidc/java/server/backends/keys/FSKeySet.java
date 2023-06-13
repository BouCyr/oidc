package app.cbo.oidc.java.server.backends.keys;

import app.cbo.oidc.java.server.backends.filesystem.FileSpecification;
import app.cbo.oidc.java.server.backends.filesystem.FileSpecifications;
import app.cbo.oidc.java.server.backends.filesystem.FileStorage;
import app.cbo.oidc.java.server.datastored.KeyId;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jwt.JWK;
import app.cbo.oidc.java.server.jwt.JWKSet;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

public record FSKeySet(FileStorage userDataFileStorage) implements KeySet {
    public static final FileSpecification KeyFile = FileSpecifications.in("keys").fileName("keySet.txt");
    /*
     String algorithm = "DSA"; // or RSA, DH, etc.

        // Generate a 1024-bit Digital Signature Algorithm (DSA) key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm);
        keyGen.initialize(1024);//from   w w  w  . j  a va 2  s  .c  o m
        KeyPair keypair = keyGen.genKeyPair();
        PrivateKey privateKey = keypair.getPrivate();
        PublicKey publicKey = keypair.getPublic();

        byte[] privateKeyBytes = privateKey.getEncoded();
        byte[] publicKeyBytes = publicKey.getEncoded();

        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        PrivateKey privateKey2 = keyFactory.generatePrivate(privateKeySpec);

        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        PublicKey publicKey2 = keyFactory.generatePublic(publicKeySpec);

        // The orginal and new keys are the same
        boolean same = privateKey.equals(privateKey2);
        same = publicKey.equals(publicKey2);
     */
    private final static Logger LOGGER = Logger.getLogger(FSKeySet.class.getCanonicalName());

    public FSKeySet(FileStorage userDataFileStorage) {
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

    private void init() {
        KeyPairGenerator kpg;
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
            //rfc7518 : A key of size 2048 bits or larger MUST be used with these algorithms.
            kpg.initialize(2048);
            var kp = kpg.generateKeyPair();


            //randomize the kid, so we do not reuse a kid (if we did, a client could store the 'old' key value in some cache)
            var currentKp = KeyId.of(UUID.randomUUID().toString());

            this.writeKeys(new KeyPair(true, currentKp, kp.getPrivate(), kp.getPublic()));

        } catch (NoSuchAlgorithmException e) {
            LOGGER.severe("NoSuchAlgorithmException when building keyset : " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void writeKeys(KeyPair... keyPairs) {
        try (var writer = this.userDataFileStorage.writer(KeyFile)) {
            for (var kp : keyPairs) {
                writer.write(this.toLine(kp));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String toLine(KeyPair kp) {
        return kp.keyId()
                + "."
                + (kp.current() ? "1" : "0")
                + "."
                + Base64.getEncoder().encodeToString(kp.privateKey().getEncoded())
                + "."
                + Base64.getEncoder().encodeToString(kp.publicKey().getEncoded());
    }

    private KeyPair fromLine(KeyFactory keyFactory, String s) {
        try {
            String[] parts = s.split(";");
            String keyId = parts[0];
            boolean current = "1".equals(parts[1]);

            var privateKeyBytes = Base64.getDecoder().decode(parts[2]);
            EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            PrivateKey pk = keyFactory.generatePrivate(privateKeySpec);

            var publicKeyBytes = Base64.getDecoder().decode(parts[2]);
            EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            PublicKey pub = keyFactory.generatePublic(publicKeySpec);

            return new KeyPair(current, KeyId.of(keyId), pk, pub);
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse keyset line", e);
        }
    }

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
    @Override
    public JWKSet asJWKSet() {

        return new JWKSet(
                this.readKeySet().stream()
                        .map(kp -> JWK.rsaPublicKey(kp.keyId().getKeyId(), (RSAPublicKey) kp.publicKey()))
                        .toList()
        );
    }

    @NotNull
    @Override
    public KeyId current() {
        return this.readKeySet().stream()
                .filter(KeyPair::current)
                .map(KeyPair::keyId)
                .findAny().orElseThrow(() -> new RuntimeException("No current key found"));
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

    record KeyPair(boolean current, KeyId keyId, PrivateKey privateKey, PublicKey publicKey) {
    }
}
