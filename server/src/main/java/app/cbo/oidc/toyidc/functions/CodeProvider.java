package app.cbo.oidc.toyidc.functions;

@FunctionalInterface
public interface CodeProvider {
     String store(String clientId, String nonce, String redirectUri, String userId);
}
