package app.cbo.oidc.java.server.http.userinfo;

import app.cbo.oidc.java.server.backends.keys.KeySet;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.jwt.JWS;
import app.cbo.oidc.java.server.jwt.JWSHeader;
import app.cbo.oidc.java.server.oidc.Issuer;
import app.cbo.oidc.java.server.oidc.tokens.AccessOrRefreshToken;
import app.cbo.oidc.java.server.utils.HttpCode;

import java.time.Clock;
import java.time.Instant;
import java.util.Set;
import java.util.logging.Logger;

public class JWTAccessTokenValidator implements AccessTokenValidator {

    private final static Logger LOGGER = Logger.getLogger(JWTAccessTokenValidator.class.getCanonicalName());
    private final KeySet keySet;
    private final Issuer me;

    public JWTAccessTokenValidator(Issuer myself, KeySet keySet) {
        this.keySet = keySet;
        this.me = myself;
    }


    @Override
    public AccessTokenData validateAccessToken(String accessToken) throws ForbiddenResponse {
        LOGGER.info("Starting validation of an access_token written as a JWT");
        //decode accestoken
        //in OUR server, accesstoken is a JWT. In another OIDC server, it could be anything (random UUID stored in DB with the relevant data,....)
        var parts = accessToken.split("\\.");
        if (parts.length != 3) {
            LOGGER.info("Provided access token is not issued by our server (invalid format)");
            throw new ForbiddenResponse(HttpCode.FORBIDDEN, ForbiddenResponse.InternalReason.UNREADABLE_TOKEN, ForbiddenResponse.INVALID_TOKEN);
        }

        var b64Metadata = parts[0];
        var b64Payload = parts[1];
        var signature = parts[2];

        // Warning ; b64 encoding is not perfectly standard in the case of JWT/OIDC ('=' padding was removed)
        var payloadBytes = JWS.base64urldecode(b64Payload);
        var payload = new String(payloadBytes);
        var decodedPayload = AccessOrRefreshToken.fromJson(payload);

        var clock = Clock.systemUTC();
        var now = Instant.now(clock).getEpochSecond();

        if (decodedPayload.exp() < now) {
            LOGGER.info("access token is expired (exp : " + decodedPayload.exp() + ", now is " + now);
            throw new ForbiddenResponse(HttpCode.UNAUTHORIZED, ForbiddenResponse.InternalReason.EXPIRED_TOKEN, ForbiddenResponse.INVALID_TOKEN);
        }

        if (!AccessOrRefreshToken.TYPE_ACCESS.equals(decodedPayload.typ())) {
            LOGGER.info("provided token is not an access token");
            throw new ForbiddenResponse(HttpCode.UNAUTHORIZED, ForbiddenResponse.InternalReason.WRONG_TYPE, ForbiddenResponse.INVALID_TOKEN);
        }
        if (!this.me.getIssuerId().equals(decodedPayload.iss())) {
            LOGGER.info("provided token has been issued by someone else");
            throw new ForbiddenResponse(HttpCode.UNAUTHORIZED, ForbiddenResponse.InternalReason.WRONG_ISSUER, ForbiddenResponse.INVALID_TOKEN);
        }


        var headerBytes = JWS.base64urldecode(b64Metadata);
        var headerJson = new String(headerBytes);
        var header = JWSHeader.fromJson(headerJson);

        if (!JWS.checkSignature(this.keySet, b64Metadata + "." + b64Payload, signature, header)) {
            LOGGER.info("Token signature invalid");
            throw new ForbiddenResponse(HttpCode.UNAUTHORIZED, ForbiddenResponse.InternalReason.INVALID_SIGNATURE, ForbiddenResponse.INVALID_TOKEN);
        }
        LOGGER.info("JWT access token is valid");
        return new AccessTokenData(UserId.of(decodedPayload.sub()), Set.copyOf(decodedPayload.scopes()));
    }
}
