package app.cbo.oidc.toyidc.backend;

public class StoredCode {
    public final String userId;
    public final String nonce;
    public final String generatedFor;
    public final String redirectUri;
    public final long issuedAt;

    //[17/11/2021] nonce ?

    public StoredCode(String userId, String nonce, String generatedFor, String redirectUri) {
        this.userId = userId;
        this.nonce = nonce;
        this.generatedFor = generatedFor;
        this.redirectUri = redirectUri;
        this.issuedAt = System.currentTimeMillis();
    }
}
