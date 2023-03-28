package app.cbo.oidc.java.server.otp;

import java.security.SecureRandom;

public class SecretGenerator {

    private static SecretGenerator instance = null;
    private SecretGenerator(){ }
    public static SecretGenerator getInstance() {
        if(instance == null){
          instance = new SecretGenerator();
        }
        return instance;
    }


    private final SecureRandom secureRandom = new SecureRandom();
    public byte[] generateSecret(){
        return this.generateSecret(160/8);
    }

    public byte[] generateSecret(int size){
        byte[] secret = new byte[size];//use 160, since I'm not sure about my Base32 padding
        this.secureRandom.nextBytes(secret);
        return secret;
    }
}
