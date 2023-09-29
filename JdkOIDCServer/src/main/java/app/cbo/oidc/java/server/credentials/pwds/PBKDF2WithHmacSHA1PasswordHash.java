package app.cbo.oidc.java.server.credentials.pwds;

import app.cbo.oidc.java.server.jsr305.NotNull;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

public class PBKDF2WithHmacSHA1PasswordHash implements Passwords{

    public PBKDF2WithHmacSHA1PasswordHash() {
    }


    public String encode(@NotNull String clear){

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

    public boolean confront(@NotNull String provided, @NotNull String storedEncoded){

        if(provided == null || storedEncoded == null)
            return false;

        var tabs = storedEncoded.split("\\.");
        var salt = Base64.getDecoder().decode(tabs[0]);
        var storedHash = Base64.getDecoder().decode(tabs[1]);
        var providedHash = this.hash(provided, salt);

        return (Arrays.compare(providedHash, storedHash)==0);

    }
}
