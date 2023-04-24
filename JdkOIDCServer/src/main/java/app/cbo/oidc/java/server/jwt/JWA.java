package app.cbo.oidc.java.server.jwt;

public enum JWA {
    RS256("RSA", "RS256", "SHA256withRSA"),
    NONE("none", "none", null);

    private final String type;
    private final String rfcName;
    private final String javaName;

    JWA(String type, String rfcName, String javaName) {
        this.type = type;
        this.rfcName = rfcName;
        this.javaName = javaName;
    }

    public String type() {
        return type;
    }

    public String rfcName() {
        return rfcName;
    }

    public String javaName() {
        return javaName;
    }
}
