package app.cbo.oidc.java.server.http.token.params;

public interface RefreshParams {
    String grantType();

    String refreshToken();
}
