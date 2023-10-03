package app.cbo.oidc.java.server.http.userinfo;

import app.cbo.oidc.java.server.backends.claims.ClaimsResolver;
import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;

import java.util.HashMap;
import java.util.logging.Logger;

public class UserInfoEndpointImpl implements UserInfoEndpoint {

    private final static Logger LOGGER = Logger.getLogger(UserInfoEndpointImpl.class.getCanonicalName());

    private final ClaimsResolver claimsResolver;
    private final AccessTokenValidator accessTokenValidator;

    public UserInfoEndpointImpl(ClaimsResolver claimsResolver, AccessTokenValidator accessTokenValidator) {
        this.claimsResolver = claimsResolver;
        this.accessTokenValidator = accessTokenValidator;
    }


    @Override
    @NotNull
    public Interaction treatRequest(String accessToken) {

        LOGGER.info("Token validation");
        AccessTokenData decodedPayload;
        try {
            decodedPayload = this.accessTokenValidator.validateAccessToken(accessToken);
        } catch (ForbiddenResponse forbiddenResponse) {
            return forbiddenResponse;
        }
        LOGGER.info("Token is valid");

        LOGGER.info("Retrieving claims of users for agreed scopes");
        var userInfo = claimsResolver.claimsFor(decodedPayload.sub(), decodedPayload.scopes());

        //5.3.2 > The sub (subject) Claim MUST always be returned in the UserInfo Response.

        if (userInfo.containsKey("sub")) {

            if (!userInfo.get("sub").equals(decodedPayload.sub().getUserId())) {
                LOGGER.warning("'sub' claims from access_token differs from claims");
                throw new RuntimeException("Unexpected data error");
            }

            return new UserInfoResponse(userInfo);
        } else {
            var copy = new HashMap<>(userInfo);//userinfo may be immutable
            copy.put("sub", decodedPayload.sub().getUserId());
            return new UserInfoResponse(copy);
        }
    }


}
