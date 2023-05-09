package app.cbo.oidc.java.server.endpoints.jwks;

import app.cbo.oidc.java.server.backends.KeySet;
import app.cbo.oidc.java.server.json.JSON;
import app.cbo.oidc.java.server.jwt.JWK;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;

class JWKTest {

    public static final String PAYLOAD = "Hello darkness my old friend";


    @Test
    public void test() throws Exception {
        var keyset = new KeySet();
        //the current kid
        var kid = keyset.current();
        //the corresponding public k
        var publicKey = keyset.publicKey(kid)
                .orElseThrow(() -> new RuntimeException("No key found, test fails"));
        //the corresponding private key
        var privateKey = keyset.privateKey(kid)
                .orElseThrow(() -> new RuntimeException("No key found, test fails"));


        //JWK built from the key pair (well public key) using nimbus lib
        var theirs = new RSAKey.Builder((RSAPublicKey) publicKey)
                .algorithm(JWSAlgorithm.RS256)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(kid.getKeyId()).build();

        String theirsAsJson = theirs.toJSONObject().toJSONString();
        //give it to jackson to do proper indent
        var readByJackson = new ObjectMapper().reader().readTree(theirsAsJson);
        System.out.println("AS COMPUTED BY EXTERNAL (nimbus):");
        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(readByJackson));

        var publicKeyFromNimbus = RSAKey.parse(theirsAsJson);

        //JWK build using our code
        var mine = JWK.rsaPublicKey(kid.getKeyId(), (RSAPublicKey) publicKey);
        var mineJson = JSON.jsonify(mine);

        System.out.println("AS COMPUTED BY OUR CODE (jdk impl.):");
        System.out.println(mineJson);

        //parsing the JWK computed by our code using nimbus
        var mineParsedByThem = RSAKey.parse(mineJson);

        //we will check that the public keys read from the JWK are valid by encrypting a payload with the public keys
        //and reading it back using the raw RSA private key.

        //-as a sanity check, we will first use the raw public key, that was not serialized as a JWK
        var encryptorFromKeySet = Cipher.getInstance("RSA");
        encryptorFromKeySet.init(Cipher.ENCRYPT_MODE, publicKey);
        var encryptedFromKeySet = encryptorFromKeySet.doFinal(PAYLOAD.getBytes(StandardCharsets.UTF_8));

        //-as a sanity check, we will then use the public key that was serialized in JWK by nimbus and then read back by nimbus
        var encryptorFromExternal = Cipher.getInstance("RSA");
        encryptorFromExternal.init(Cipher.ENCRYPT_MODE, publicKeyFromNimbus.toRSAPublicKey());
        var encryptedFromExternal = encryptorFromExternal.doFinal("Hello darkness my old friend".getBytes(StandardCharsets.UTF_8));

        //-as a sanity check, we will then use the public key that was serialized in JWK by OUR code and then read back by nimbus
        var encryptorFromMineJWK = Cipher.getInstance("RSA");
        encryptorFromMineJWK.init(Cipher.ENCRYPT_MODE, mineParsedByThem.toRSAPublicKey());
        var encryptedFromMineJWK = encryptorFromMineJWK.doFinal("Hello darkness my old friend".getBytes(StandardCharsets.UTF_8));

        String decryptedFromKeySet = decrypt(privateKey, encryptedFromKeySet);
        String decryptedFromExternal = decrypt(privateKey, encryptedFromExternal);
        String decryptedFromMineJWK = decrypt(privateKey, encryptedFromMineJWK);

        //all decrypted strings must be equal to the original string
        Assertions.assertThat(PAYLOAD)
                .isEqualTo(decryptedFromExternal)
                .isEqualTo(decryptedFromKeySet)
                .isEqualTo(decryptedFromMineJWK);


    }

    private String decrypt(PrivateKey privateKey, byte[] encryptedFromKeySet) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        var decryptorFromKeySet = Cipher.getInstance("RSA");
        decryptorFromKeySet.init(Cipher.DECRYPT_MODE, privateKey);
        var decryptedFormKeySet = decryptorFromKeySet.doFinal(encryptedFromKeySet);
        return new String(decryptedFormKeySet, StandardCharsets.UTF_8);
    }
}