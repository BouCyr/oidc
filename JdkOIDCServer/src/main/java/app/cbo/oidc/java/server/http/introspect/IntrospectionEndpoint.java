package app.cbo.oidc.java.server.http.introspect;

import app.cbo.oidc.java.server.backends.clients.ClientRegistry;
import app.cbo.oidc.java.server.backends.tokens.AccessTokenValidator;
import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.http.JsonResponse;
import app.cbo.oidc.java.server.http.token.JsonError;
import app.cbo.oidc.java.server.http.userinfo.ForbiddenResponse;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;
import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.utils.HttpCode;
import app.cbo.oidc.java.server.utils.Utils;

import java.util.HashMap;
import java.util.logging.Logger;

@Injectable
public class IntrospectionEndpoint {

    private static final Logger LOGGER = Logger.getLogger(IntrospectionEndpoint.class.getCanonicalName());
    private final ClientRegistry clientRegistry;
    private final AccessTokenValidator accessTokenValidator;

    public IntrospectionEndpoint(ClientRegistry clientRegistry, AccessTokenValidator accessTokenValidator) {
        this.clientRegistry = clientRegistry;
        this.accessTokenValidator = accessTokenValidator;
    }

    public Interaction treatRequest(@NotNull String token,
                                    @NotNull ClientId authClientId,
                                    @Nullable String clientSecret) throws JsonError, ForbiddenResponse {

        if (!this.clientRegistry.authenticate(authClientId, clientSecret)) {
            LOGGER.warning("client was not authenticated");
            throw new ForbiddenResponse(HttpCode.UNAUTHORIZED, ForbiddenResponse.InternalReason.INVALID_CREDENTIALS, "invalid client credentials");
        }

        var tokenData = this.accessTokenValidator.validateAccessToken(token);
        LOGGER.warning("Token validation result : token is " + (tokenData.active() ? "ACTIVE" : "INACTIVE"));

        var jsonKV = new HashMap<>();
        jsonKV.put("active", tokenData.active());
        if (tokenData.active()) {
            jsonKV.put("scope", String.join(" ", tokenData.scopes()));
            jsonKV.put("client_id", tokenData.clientId().id());
            jsonKV.put("username", tokenData.sub().id());
            jsonKV.put("token_type", "Bearer"); //todo refresh
            jsonKV.put("exp", tokenData.exp());
            jsonKV.put("iat", tokenData.iat());
            jsonKV.put("sub", tokenData.sub().id());
            if (!Utils.isBlank(tokenData.aud())) {
                jsonKV.put("aud", tokenData.aud());
            }
            jsonKV.put("iss", tokenData.iss());
            jsonKV.put("jti", tokenData.jti());
        }
        return new JsonResponse(jsonKV);
    }


}
