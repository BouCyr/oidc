package app.cbo.oidc.java.server.jwt;

import app.cbo.oidc.java.server.json.JSON;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

public class JWS {


    //TODO [13/04/2023] only pass keyId, switch to state component with a dep. on keystore
    public static String jwsWrap(@NotNull JWA algo, @NotNull Object payload, @Nullable String keyId, @Nullable PrivateKey key) {

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
        var signature = Base64.getUrlEncoder().encodeToString(s);

        return signedPart + "." + signature;


    }

    static String toJWSPart(Object o) {
        return Base64.getUrlEncoder().encodeToString(JSON.jsonify(o).getBytes(StandardCharsets.UTF_8));
    }

}
