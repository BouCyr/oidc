package app.cbo.oidc.java.server.http.token.endpoints;

import app.cbo.oidc.java.server.backends.clients.ClientAuthenticator;
import app.cbo.oidc.java.server.backends.keys.KeySet;
import app.cbo.oidc.java.server.backends.sessions.SessionFinder;
import app.cbo.oidc.java.server.backends.tokens.AccessTokenGenerator;
import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.http.JsonResponse;
import app.cbo.oidc.java.server.http.token.JsonError;
import app.cbo.oidc.java.server.http.token.RefreshResponse;
import app.cbo.oidc.java.server.http.token.params.RefreshParams;
import app.cbo.oidc.java.server.http.userinfo.ForbiddenResponse;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;
import app.cbo.oidc.java.server.jwt.JWS;
import app.cbo.oidc.java.server.oidc.Constants;
import app.cbo.oidc.java.server.oidc.Issuer;
import app.cbo.oidc.java.server.oidc.tokens.RefreshToken;
import app.cbo.oidc.java.server.scan.Injectable;

import java.time.Duration;
import java.util.logging.Logger;

import static app.cbo.oidc.java.server.utils.Utils.isBlank;
import static app.cbo.oidc.java.server.utils.Utils.isEmpty;

/**
 * <quote><u>cf. RFC6749</u></quote>
 *
 * <p>If the authorization server issued a refresh token to the client, the
 * client makes a refresh request to the token endpoint by adding the
 * following parameters using the "application/x-www-form-urlencoded"
 * format per Appendix B with a character encoding of UTF-8 in the HTTP
 * request entity-body:</p>
 *
 * <ul>
 *    <li>grant_type :  <b>REQUIRED</b>.  Value MUST be set to "refresh_token".</li>
 *    <li>refresh_token : <b>REQUIRED</b>.  The refresh token issued to the client.</li>
 *    <li>scope : <b>OPTIONAL</b>.  The scope of the access request as described by
 *          Section 3.3.  The requested scope MUST NOT include any scope
 *          not originally granted by the resource owner, and if omitted is
 *          treated as equal to the scope originally granted by the
 *          resource owner.</li>
 * </ul>
 *
 * <p>
 *    Because refresh tokens are typically long-lasting credentials used to
 *    request additional access tokens, the refresh token is bound to the
 *    client to which it was issued.  If the client type is confidential or
 *    the client was issued client credentials (or assigned other
 *    authentication requirements), the client MUST authenticate with the
 *    authorization server as described in Section 3.2.1.
 * </p>
 * <p>
 *    For example, the client makes the following HTTP request using
 *    transport-layer security (with extra line breaks for display purposes
 *    only):
 *    </p>
 *
 * <pre>
 *      POST /token HTTP/1.1
 *      Host: server.example.com
 *      Authorization: Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW
 *      Content-Type: application/x-www-form-urlencoded
 *
 *      grant_type=refresh_token&refresh_token=tGzv3JOkF0XG5Qx2TlKWIA
 *
 * </pre>
 *
 *    <p>The authorization server MUST:</p>
 * <ul>
 *     <li>require client authentication for confidential clients or for any
 *       client that was issued client credentials (or with other
 *       authentication requirements),</li>
 *
 *    <li>authenticate the client if client authentication is included and
 *       ensure that the refresh token was issued to the authenticated
 *       client, and</li>
 *
 *    <li>  validate the refresh token.</li>
 *
 *    <p>If valid and authorized, the authorization server issues an access
 *    token as described in Section 5.1.  If the request failed
 *    verification or is invalid, the authorization server returns an error
 *    response as described in Section 5.2.</p>
 *
 *    <p>The authorization server MAY issue a new refresh token, in which case
 *    the client MUST discard the old refresh token and replace it with the
 *    new refresh token.  The authorization server MAY revoke the old
 *    refresh token after issuing a new refresh token to the client.  If a
 *    new refresh token is issued, the refresh token scope MUST be
 *    identical to that of the refresh token included by the client in the request.</p>
 */
@Injectable
public class RefreshTokenToTokensEndpointImpl implements RefreshTokenToTokensEndpoint {

    private final static Logger LOGGER = Logger.getLogger(RefreshTokenToTokensEndpointImpl.class.getCanonicalName());

    private final ClientAuthenticator clientAuthenticator;
    private final Issuer myself;
    private final KeySet myKeys;
    private final SessionFinder sessionFinder;
    private final AccessTokenGenerator accessTokenGenerator;

    public RefreshTokenToTokensEndpointImpl(ClientAuthenticator clientAuthenticator, Issuer myself, KeySet myKeys, SessionFinder sessionFinder, AccessTokenGenerator accessTokenGenerator) {
        this.clientAuthenticator = clientAuthenticator;
        this.myself = myself;
        this.myKeys = myKeys;
        this.sessionFinder = sessionFinder;
        this.accessTokenGenerator = accessTokenGenerator;
    }

    @NotNull
    @Override
    public Interaction treatRequest(@NotNull RefreshParams params, @Nullable ClientId authClientId, @Nullable String clientSecret) {
        if (authClientId != null) {
            LOGGER.info(("'" + (!isEmpty(authClientId.get()) ? authClientId : "?") + "' tries to consume a code"));

            //Are the client credentials OK ? (none would be OK for the moment)
            if (!this.clientAuthenticator.authenticate(authClientId, clientSecret)) {
                LOGGER.info("Invalid client credentials");
                return new JsonError(JsonError.Cause.invalid_client);
            }
            LOGGER.info("Client is authenticated");
        }

        if (isBlank(params.grantType())) {
            LOGGER.warning("grantType was not present");
            return new JsonError(JsonError.Cause.invalid_client);
        }

        if (!params.grantType().equals(Constants.GrantType.REFRESH_TOKEN)) {
            LOGGER.warning("grantType was not refresh_token");
            return new JsonError(JsonError.Cause.invalid_request);
        }

        if (isBlank(params.refreshToken())) {
            LOGGER.warning("refreshToken was not provided");
            return new JsonError(JsonError.Cause.invalid_grant);
        }

        RefreshToken refreshTokenData;
        try {
            refreshTokenData = JWS.validateAndReadJWS(this.myself, this.myKeys, params.refreshToken(), RefreshToken::fromJson);
        } catch (ForbiddenResponse e) {
            return new JsonError(JsonError.Cause.invalid_request, e.getMessage());
        }

        var sessionId = SessionId.of(refreshTokenData.session_id());
        var session = this.sessionFinder.find(sessionId);
        if (session.isEmpty()) {
            LOGGER.warning("could not find session corresponding to refresh token");
            return new JsonError(JsonError.Cause.invalid_grant);
        }

        var at = accessTokenGenerator.generate(authClientId, session.get(),
                null //params.resource()
        );

        var response = new RefreshResponse(
                at,
                Duration.ofMinutes(5L),
                session.get().scopes());


        return new JsonResponse(response);
    }
}
