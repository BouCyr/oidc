package app.cbo.oidc.java.server.jwt;

import app.cbo.oidc.java.server.json.JSON;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

public class JWS {


    public static String jwsWrap(@NotNull JWA algo,
                                 @NotNull Object payload,
                                 @Nullable String keyId,
                                 @Nullable PrivateKey key) {

        if (algo != JWA.NONE && (keyId == null || key == null)) {
            throw new NullPointerException("Key & keyId must be provided if alg is not 'none'");
        }

        JWSHeader header = new JWSHeader(algo.rfcName(), "JWT", keyId);

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
            throw new RuntimeException(e);//TODO [13/04/2023]
        }
        var signature = base64urlencode(s);

        return signedPart + "." + signature;


    }

    static String toJWSPart(Object o) {
        return base64urlencode(JSON.jsonify(o).getBytes(StandardCharsets.UTF_8));
    }

    //cf https://datatracker.ietf.org/doc/html/rfc7515#appendix-C
    static String base64urlencode(byte[] arg) {
        String s = Base64.getEncoder().encodeToString(arg); // Regular base64 encoder
        s = s.split("=")[0]; // Remove any trailing '='s
        s = s.replace('+', '-'); // 62nd char of encoding
        s = s.replace('/', '_'); // 63rd char of encoding
        return s;
    }

    //cf https://datatracker.ietf.org/doc/html/rfc7515#appendix-C
    static byte[] base64urldecode(String arg) {
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
