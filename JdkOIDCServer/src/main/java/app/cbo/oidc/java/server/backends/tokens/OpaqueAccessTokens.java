package app.cbo.oidc.java.server.backends.tokens;

import app.cbo.oidc.java.server.backends.keys.KeySet;
import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.http.userinfo.ForbiddenResponse;
import app.cbo.oidc.java.server.oidc.Issuer;
import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.utils.HttpCode;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Injectable("opaqueAT")
public class OpaqueAccessTokens implements AccessTokens {

    //we will not store access tokens. We just want some tokens that the clients will not try to interpret offline
    //So we take the JWT tokens and obfuscate it by doing a second pass of B64 on the whole token
    private final JWTAccessTokens innerJwtAccessTokens;


    public OpaqueAccessTokens(Issuer myself, KeySet keySet) {
        this.innerJwtAccessTokens = new JWTAccessTokens(
                myself,
                keySet
        );
    }

    @Override
    public String generate(ClientId requestedBy, Session activeSession, String resource) {
        var clear = this.innerJwtAccessTokens.generate(requestedBy, activeSession, resource);

        return Base64.getEncoder().encodeToString(clear.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public AccessTokenData validateAccessToken(String accessToken) throws ForbiddenResponse {

        String clear;
        try {
            clear = new String(Base64.getDecoder().decode(accessToken.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new ForbiddenResponse(HttpCode.UNAUTHORIZED, ForbiddenResponse.InternalReason.UNREADABLE_TOKEN, "invalid token");
        }
        return this.innerJwtAccessTokens.validateAccessToken(clear);
    }
}
