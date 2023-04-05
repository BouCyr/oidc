package app.cbo.oidc.java.server.credentials;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

public class PasswordEncoder {

    private static PasswordEncoder instance = null;
    public static PasswordEncoder getInstance() {
        if(instance == null){
          instance = new PasswordEncoder();
        }
        return instance;
    }


    private PasswordEncoder(){ }



    //TODO [31/03/2023] PBKDF
    public String encodePassword(String clear){

        if(clear == null){
            throw new NullPointerException("Input cannot be null");
        }

        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        var hash = hash(clear, salt);
        return Base64.getEncoder().encodeToString(salt)+"."+Base64.getEncoder().encodeToString(hash);
    }

    private byte[] hash(String clear, byte[] salt) {
        KeySpec spec = new PBEKeySpec(clear.toCharArray(), salt, 65536, 128);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return factory.generateSecret(spec).getEncoded();
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException("unable to hash password",e);
        }
    }

    public boolean confront(String provided, String storedEncoded){

        if(provided == null || storedEncoded == null)
            throw new NullPointerException("Input cannot be null");


        var tabs = storedEncoded.split("\\.");
        var salt = Base64.getDecoder().decode(tabs[0]);
        var storedHash = Base64.getDecoder().decode(tabs[1]);
        var providedHash = this.hash(provided, salt);

        return (Arrays.compare(providedHash, storedHash)==0);

    }
}
