package app.cbo.oidc.java.server.backends.tokens;

import app.cbo.oidc.java.server.backends.keys.KeySet;
import app.cbo.oidc.java.server.credentials.AuthenticationLevel;
import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.http.userinfo.ForbiddenResponse;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;
import app.cbo.oidc.java.server.jwt.JWA;
import app.cbo.oidc.java.server.jwt.JWS;
import app.cbo.oidc.java.server.oidc.Issuer;
import app.cbo.oidc.java.server.scan.Injectable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Metadata header : 'typ' should be 'at+JWT'
 * <p>
 * Claims :
 * iss REQUIRED - as defined in Section 4.1.1 of [RFC7519].
 * <p>
 * exp REQUIRED - as defined in Section 4.1.4 of [RFC7519].
 * <p>
 * aud REQUIRED - as defined in Section 4.1.3 of [RFC7519]. See Section 3 for indications on how an authorization server should determine the value of "aud" depending on the request.
 * <p>
 * sub REQUIRED - as defined in Section 4.1.2 of [RFC7519]. In cases of access tokens obtained through grants where a resource owner is involved, such as the authorization code grant, the value of "sub" SHOULD correspond to the subject identifier of the resource owner. In cases of access tokens obtained through grants where no resource owner is involved, such as the client credentials grant, the value of "sub" SHOULD correspond to an identifier the authorization server uses to indicate the client application. See Section 5 for more details on this scenario. Also, see Section 6 for a discussion about how different choices in assigning "sub" values can impact privacy.
 * <p>
 * client_id REQUIRED - as defined in Section 4.3 of [RFC8693].
 * <p>
 * iat REQUIRED - as defined in Section 4.1.6 of [RFC7519]. This claim identifies the time at which the JWT access token was issued.
 * <p>
 * jti REQUIRED - as defined in Section 4.1.7 of [RFC7519].
 * <p>
 * auth_time OPTIONAL - as defined in Section 2 of [OpenID.Core].
 * acr OPTIONAL - as defined in Section 2 of [OpenID.Core].
 * amr OPTIONAL - as defined in Section 2 of [OpenID.Core].
 */
@Injectable
public class JWTAccessTokens implements AccessTokenGenerator, AccessTokenValidator {

    private final static Logger LOGGER = Logger.getLogger(JWTAccessTokens.class.getCanonicalName());

    public static final Duration TTL = Duration.ofMinutes(5L);
    private final Issuer myself;
    private final KeySet keySet;

    public JWTAccessTokens(Issuer myself, KeySet keySet) {
        this.myself = myself;
        this.keySet = keySet;
    }

    @Override
    public AccessTokenData validateAccessToken(String accessToken) throws ForbiddenResponse {
        LOGGER.info("Starting validation of an access_token written as a JWT");
        //decode accesstoken

        final var decodedPayload = JWS.validateAndReadJWS(this.myself, this.keySet, accessToken, JWTAccessToken::fromJson);
        LOGGER.info("JWT access token is valid");
        return new AccessTokenData(
                true,
                new HashSet<>(decodedPayload.scopes()),
                ClientId.of(decodedPayload.clientId()),
                UserId.of(decodedPayload.sub()),
                "Bearer",
                decodedPayload.exp(),
                decodedPayload.iat(),
                decodedPayload.nbf(),
                decodedPayload.aud(),
                Issuer.of(decodedPayload.iss()),
                decodedPayload.jti()

        );
    }

    @Override
    public String generate(
            @NotNull ClientId requestedBy,
            @NotNull Session activeSession,
            @Nullable String resource) {

        Map<String, Object> jsonKv = new HashMap<>();
        jsonKv.put("iss", this.myself.getIssuerId());
        jsonKv.put("exp", Instant.now(Clock.systemUTC()).plus(TTL).getEpochSecond());
        jsonKv.put("aud", resource != null ? resource : requestedBy.id());
        jsonKv.put("sub", activeSession.userId().id());
        jsonKv.put("client_id", requestedBy.id());
        jsonKv.put("iat", Instant.now(Clock.systemUTC()).getEpochSecond());
        jsonKv.put("jti", UUID.randomUUID().toString());
        jsonKv.put("auth_time", activeSession.authTime().toEpochSecond(ZoneOffset.UTC));
        jsonKv.put("acr", new AuthenticationLevel(activeSession.authentications()).level());
        jsonKv.put("amr", activeSession.authentications().stream().map(Enum::name).toList());

        if (!activeSession.scopes().isEmpty()) {
            jsonKv.put("scopes", activeSession.scopes());
        }

        var currentPrivateKeyId = this.keySet.current();
        var currentPrivateKey = this.keySet.privateKey(currentPrivateKeyId)
                .orElseThrow(() -> new RuntimeException("No current private key found (?)"));

        //RFC 9068 2.1 : WT access tokens MUST be signed. [...]  the "typ" value used SHOULD be "at+jwt".
        var jwt = JWS.jwsWrap(JWA.RS256, jsonKv, currentPrivateKeyId, currentPrivateKey, "at+jwt");

        return jwt;
    }
}
