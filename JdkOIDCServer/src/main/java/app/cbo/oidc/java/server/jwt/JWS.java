package app.cbo.oidc.java.server.jwt;

import app.cbo.oidc.java.server.backends.keys.KeySet;
import app.cbo.oidc.java.server.datastored.KeyId;
import app.cbo.oidc.java.server.json.JSON;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;
import java.util.logging.Logger;

public class JWS {

    private final static Logger LOGGER = Logger.getLogger(JWS.class.getCanonicalName());

    public static boolean checkSignature(KeySet keyset, String signed, String b64signature, JWSHeader header) {

        var foundAlgo = JWA.fromRFC(header.alg());
        if (foundAlgo.isEmpty()) {
            LOGGER.info("alg '" + header.alg() + "' is not a supported JWA algorithm");
            return false;
        }
        var algo = foundAlgo.get();

        var foundKey = keyset.publicKey(KeyId.of(header.kid()));
        if (foundKey.isEmpty()) {
            LOGGER.info("key '" + header.kid() + "' is not a known key");
            return false;
        }
        var key = foundKey.get();

        try {
            Signature privateSignature = Signature.getInstance(algo.javaName());
            privateSignature.initVerify(key);
            privateSignature.update(signed.getBytes(StandardCharsets.UTF_8));
            return privateSignature.verify(base64urldecode(b64signature));
        } catch (Exception e) {
            LOGGER.info("Invalid signature");
            return false;
        }

    }


    public static String jwsWrap(@NotNull JWA algo,
                                 @NotNull Object payload,
                                 @Nullable KeyId keyId,
                                 @Nullable PrivateKey key) {

        if (algo != JWA.NONE && (keyId == null || key == null)) {
            throw new NullPointerException("Key & keyId must be provided if alg is not 'none'");
        }


        JWSHeader header = new JWSHeader(algo.rfcName(), "JWT", keyId != null ? keyId.getKeyId() : null);

        String signedPart = toJWSPart(header) + "." + toJWSPart(payload);

        if (algo == JWA.NONE) {
            //if no alg, there will be no signature part. The '.' is still there.
            return signedPart + ".";
        }
        byte[] s;
        try {
            Signature privateSignature = Signature.getInstance(algo.javaName());
            privateSignature.initSign(key);
            privateSignature.update(signedPart.getBytes(StandardCharsets.UTF_8));
            s = privateSignature.sign();
        } catch (Exception e) {
            LOGGER.severe("Exception while computing JWS signature : " + e.getMessage());
            throw new RuntimeException(e);
        }
        var signature = base64urlencode(s);

        return signedPart + "." + signature;


    }

    static String toJWSPart(Object o) {
        return base64urlencode(JSON.jsonify(o).getBytes(StandardCharsets.UTF_8));
    }

    //cf https://datatracker.ietf.org/doc/html/rfc7515#appendix-C
    public static String base64urlencode(byte[] arg) {
        String s = Base64.getEncoder().encodeToString(arg); // Regular base64 encoder
        s = s.split("=")[0]; // Remove any trailing '='s
        s = s.replace('+', '-'); // 62nd char of encoding
        s = s.replace('/', '_'); // 63rd char of encoding
        return s;
    }

    //cf https://datatracker.ietf.org/doc/html/rfc7515#appendix-C
    public static byte[] base64urldecode(String arg) {
        String s = arg;
        s = s.replace('-', '+'); // 62nd char of encoding
        s = s.replace('_', '/'); // 63rd char of encoding
        switch (s.length() % 4) // Pad with trailing '='s
        {
            case 0:
                break; // No pad chars in this case
            case 2:
                s += "==";
                break; // Two pad chars
            case 3:
                s += "=";
                break; // One pad char
            default:
                throw new RuntimeException(
                        "Illegal base64url string!");
        }
        return Base64.getDecoder().decode(s); // Standard base64 decoder
    }

}
