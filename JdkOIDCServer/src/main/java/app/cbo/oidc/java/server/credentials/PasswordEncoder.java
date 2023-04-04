package app.cbo.oidc.java.server.credentials;

public class PasswordEncoder {

    //TODO [31/03/2023] PBKDF
    public static String encodePassword(String clear){
        return clear;
    }

    public static boolean confront(String provided, String storedEncoded){
        if(provided == null) {
            return storedEncoded==null;
        }

        return provided.equals(storedEncoded);
    }
}
