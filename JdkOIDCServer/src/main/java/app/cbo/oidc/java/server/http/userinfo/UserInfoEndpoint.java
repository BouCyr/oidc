package app.cbo.oidc.java.server.http.userinfo;

import app.cbo.oidc.java.server.backends.claims.ClaimsResolver;
import app.cbo.oidc.java.server.backends.keys.KeySet;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jwt.JWS;
import app.cbo.oidc.java.server.jwt.JWSHeader;
import app.cbo.oidc.java.server.oidc.tokens.AccessOrRefreshToken;
import app.cbo.oidc.java.server.utils.HttpCode;

import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.logging.Logger;

public class UserInfoEndpoint {

    private final static Logger LOGGER = Logger.getLogger(UserInfoEndpoint.class.getCanonicalName());

    private final ClaimsResolver claimsResolver;
    private final KeySet keySet;

    public UserInfoEndpoint(ClaimsResolver claimsResolver, KeySet keySet) {
        this.claimsResolver = claimsResolver;
        this.keySet = keySet;
    }


    @NotNull
    public Interaction treatRequest(String accessToken) {


        //decode accestoken
        //in OUR server, accesstoken is a JWT. In another OIDC server, it could be anything (random UUID stored in DB with the relevant data,....)
        var parts = accessToken.split("\\.");
        if (parts.length != 3) {
            LOGGER.info("Provided access token is not issued by our server (invalid format)");
            return new ForbiddenResponse(HttpCode.FORBIDDEN, ForbiddenResponse.INVALID_TOKEN);
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
            return new ForbiddenResponse(HttpCode.UNAUTHORIZED, ForbiddenResponse.INVALID_TOKEN);
        }

        if (!AccessOrRefreshToken.TYPE_ACCESS.equals(decodedPayload.typ())) {
            LOGGER.info("provided token is not an access token");
            return new ForbiddenResponse(HttpCode.UNAUTHORIZED, ForbiddenResponse.INVALID_TOKEN);
        }

        var headerBytes = JWS.base64urldecode(b64Metadata);
        var headerJson = new String(headerBytes);
        var header = JWSHeader.fromJson(headerJson);

        if (!JWS.checkSignature(this.keySet, b64Metadata + "." + b64Payload, signature, header)) {
            LOGGER.info("Token signature invalid");
            return new ForbiddenResponse(HttpCode.UNAUTHORIZED, ForbiddenResponse.INVALID_TOKEN);
        }
        LOGGER.info("Token signature is valid");

        LOGGER.info("Retrieving claims of users for agreed scopes");
        var userInfo = claimsResolver.claimsFor(UserId.of(decodedPayload.sub()), new HashSet<>(decodedPayload.scopes()));

        //5.3.2 > The sub (subject) Claim MUST always be returned in the UserInfo Response.
        userInfo.put("sub", decodedPayload.sub());
        return new UserInfoResponse(userInfo);
    }
}
