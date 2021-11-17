package app.cbo.oidc.toyidc.payloads;

public class TokenResponse extends Response{
    public String access_token;
    public String token_type;
    public String refresh_token;
    public long expires_in;
    public String id_token;

}
