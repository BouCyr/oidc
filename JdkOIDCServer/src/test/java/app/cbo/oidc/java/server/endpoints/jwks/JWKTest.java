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
        //the current kid
        var kid = KeySet.getInstance().current();
        //the corresponding public k
        var publicKey = KeySet.getInstance().publicKey(kid)
                .orElseThrow(() -> new RuntimeException("No key found, test fails"));
        //the correpsonding private key
        var privateKey = KeySet.getInstance().privateKey(kid)
                .orElseThrow(() -> new RuntimeException("No key found, test fails"));


        //JWK built from the key pair (well public key) using nimbus lib
        var theirs = new RSAKey.Builder((RSAPublicKey) publicKey)
                .algorithm(JWSAlgorithm.RS256)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(kid.getKeyId()).build();

        String theirsAsJson = theirs.toJSONObject().toJSONString();
        //give it to jackson to do proper ident
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
        var encryptorfromKeySet = Cipher.getInstance("RSA");
        encryptorfromKeySet.init(Cipher.ENCRYPT_MODE, publicKey);
        var encryptedFromKeySet = encryptorfromKeySet.doFinal(PAYLOAD.getBytes(StandardCharsets.UTF_8));

        //-as a sanity check, we will then use the public key that was serialized in JWK by nimbus and then read back by nimbus
        var encryptorfromExternal = Cipher.getInstance("RSA");
        encryptorfromExternal.init(Cipher.ENCRYPT_MODE, publicKeyFromNimbus.toRSAPublicKey());
        var encryptedFromExternal = encryptorfromExternal.doFinal("Hello darkness my old friend".getBytes(StandardCharsets.UTF_8));

        //-as a sanity check, we will then use the public key that was serialized in JWK by OUR code and then read back by nimbus
        var encryptorfromMineJWK = Cipher.getInstance("RSA");
        encryptorfromMineJWK.init(Cipher.ENCRYPT_MODE, mineParsedByThem.toRSAPublicKey());
        var encryptedFromMineJWK = encryptorfromMineJWK.doFinal("Hello darkness my old friend".getBytes(StandardCharsets.UTF_8));

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
        var decryptorFromKeyset = Cipher.getInstance("RSA");
        decryptorFromKeyset.init(Cipher.DECRYPT_MODE, privateKey);
        var decryptedFormKeyset = decryptorFromKeyset.doFinal(encryptedFromKeySet);
        var asString = new String(decryptedFormKeyset, StandardCharsets.UTF_8);
        return asString;
    }
}