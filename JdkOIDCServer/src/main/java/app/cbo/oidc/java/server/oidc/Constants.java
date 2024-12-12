package app.cbo.oidc.java.server.oidc;

public class Constants {

    public static class GrantType {

        public static final String AUTHORIZATION_CODE = "authorization_code";
        public static final String REFRESH_TOKEN = "refresh_token";
    }

    public static class Scope {
        public static final String PROFILE = "profile";
        public static final String ADDRESS = "address";
        public static final String MAIL = "mail";
        public static final String PHONE = "phone";
    }
}
