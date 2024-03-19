package app.cbo.oidc.java.server.credentials;

import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.scan.Injectable;

import java.security.SecureRandom;

/**
 * Generates securely a random byte array (used for TOTP secret generation)
 */
@Injectable
public class SecretGenerator {
    private final SecureRandom secureRandom = new SecureRandom();

    public SecretGenerator(){ }

    @NotNull public byte[] generateSecret(){
        return this.generateSecret(160/8);
    }

    @NotNull public byte[] generateSecret(int size){
        byte[] secret = new byte[size];//use 160, since I'm not sure about my Base32 padding
        this.secureRandom.nextBytes(secret);
        return secret;
    }
}
