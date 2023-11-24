package app.cbo.oidc.java.server.http.token;

import app.cbo.oidc.java.server.backends.codes.CodeConsumer;
import app.cbo.oidc.java.server.backends.keys.KeySet;
import app.cbo.oidc.java.server.backends.sessions.SessionFinder;
import app.cbo.oidc.java.server.backends.users.UserFinder;
import app.cbo.oidc.java.server.credentials.AuthenticationLevel;
import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Code;
import app.cbo.oidc.java.server.http.AuthErrorInteraction;
import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.http.userinfo.ForbiddenResponse;
import app.cbo.oidc.java.server.json.JSON;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;
import app.cbo.oidc.java.server.jwt.JWA;
import app.cbo.oidc.java.server.jwt.JWS;
import app.cbo.oidc.java.server.oidc.Issuer;
import app.cbo.oidc.java.server.oidc.tokens.AccessOrRefreshToken;
import app.cbo.oidc.java.server.oidc.tokens.IdToken;
import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.utils.Utils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Injectable
public class TokenEndpointImpl implements TokenEndpoint {

    private final static Logger LOGGER = Logger.getLogger(TokenEndpointImpl.class.getCanonicalName());

    private final Issuer myself;
    private final CodeConsumer codeConsumer;
    private final UserFinder userFinder;
    private final SessionFinder sessionFinder;
    private final KeySet keySet;

    public TokenEndpointImpl(Issuer myself, CodeConsumer codeConsumer, UserFinder userFinder, SessionFinder sessionFinder, KeySet keySet) {
        this.myself = myself;
        this.codeConsumer = codeConsumer;
        this.userFinder = userFinder;
        this.sessionFinder = sessionFinder;
        this.keySet = keySet;
    }

    @Override
    @NotNull
    public Interaction treatRequest(@NotNull TokenParams params, @Nullable String authClientId, @Nullable String clientSecret) throws JsonError, ForbiddenResponse {
        /*
        The Authorization Server MUST validate the Token Request as follows:

        Authenticate the Client if it was issued Client Credentials or if it uses another Client Authentication method, per Section 9.
        Ensure the Authorization Code was issued to the authenticated Client.
        Verify that the Authorization Code is valid.
        If possible, verify that the Authorization Code has not been previously used.
        Ensure that the redirect_uri parameter value is identical to the redirect_uri parameter value that was included in the initial Authorization Request. If the redirect_uri parameter value is not present when there is only one registered redirect_uri value, the Authorization Server MAY return an error (since the Client should have included the parameter) or MAY proceed without an error (since OAuth 2.0 permits the parameter to be omitted in this case).
        Verify that the Authorization Code used was issued in response to an OpenID Connect Authentication Request (so that an ID Token will be returned from the Token Endpoint).
        */

        LOGGER.info(("'" + (!Utils.isEmpty(authClientId) ? authClientId : "?") + "' tries to consume a code"));

        //Are the client credentials OK ? (none would be OK for the moment)
        if (!this.authenticateClient(authClientId, clientSecret)) {
            LOGGER.info("Invalid client credentials");
            return new JsonError(AuthErrorInteraction.Code.access_denied.name());
        }
        LOGGER.info("Client is authenticated");


        //have we at least one clientId somewhere ?
        if (Utils.isBlank(authClientId) && Utils.isEmpty(params.clientId())) {
            LOGGER.info("Client id not present");
            return new JsonError("clientid not present");
        }

        //the clientId may be found in credentials OR in the params.
        //we already check that we have at least one, and if two that they match
        var clientId = ClientId.of(authClientId != null ? authClientId : params.clientId());


        if (Utils.isEmpty(params.redirectUri())) {
            return new JsonError("redirecturi not present");
        }
        if (Utils.isEmpty(params.grantType())) {
            return new JsonError("grant type not present");
        }
        if (!params.grantType().equals("authorization_code")) {
            return new JsonError("invalid grant type");
        }


        var codeData = this.codeConsumer.consume(Code.of(params.code()), clientId, URLDecoder.decode(params.redirectUri(), StandardCharsets.UTF_8));
        if (codeData.isEmpty()) {
            return new JsonError(AuthErrorInteraction.Code.access_denied.name());
        }

        LOGGER.info("Code is retrieved");

        var user = this.userFinder.find(codeData.get().userId());
        if (user.isEmpty()) {
            return new JsonError(AuthErrorInteraction.Code.user_not_found.name());
        }
        LOGGER.info("User is found");

        var session = this.sessionFinder.find(codeData.get().sessionId());
        if (session.isEmpty()) {
            return new JsonError(AuthErrorInteraction.Code.session_not_found.name());
        }
        LOGGER.info("User session the code was generated from is found");


        var clock = Clock.systemUTC();

        var idToken = new IdToken(
                user.get().sub(),
                this.myself.getIssuerId(),
                List.of(clientId.getClientId()),
                Instant.now(clock).plus(Duration.ofMinutes(5L)).getEpochSecond(),
                Instant.now(clock).getEpochSecond(),
                session.get().authTime().toEpochSecond(ZoneOffset.UTC),
                Optional.ofNullable(codeData.get().nonce()),
                new AuthenticationLevel(session.get().authentications()).name(),
                session.get().authentications().stream().map(Enum::name).toList(),
                Optional.of(clientId.getClientId()),
                new HashMap<>());


        idToken.extranodes().put("at_hash", "rooooo"); //TODO [25/04/2023] at_hash management
        LOGGER.info("idToken is : " + JSON.jsonify(idToken));

        //access and refresh tokens will be transmitted as JWS, so we do not have to store them
        //any token received will be valid if signature is OK.
        var accessToken = new AccessOrRefreshToken(
                this.myself.getIssuerId(),
                AccessOrRefreshToken.TYPE_ACCESS,
                user.get().sub(),
                Instant.now(clock).plus(Duration.ofMinutes(5L)).getEpochSecond(),
                codeData.get().scopes());
        var refreshToken = new AccessOrRefreshToken(
                this.myself.getIssuerId(),
                AccessOrRefreshToken.TYPE_REFRESH,
                user.get().sub(),
                Instant.now(clock).plus(Duration.ofMinutes(5L)).getEpochSecond(),
                codeData.get().scopes());

        var currentPrivateKeyId = this.keySet.current();
        var currentPrivateKey = this.keySet.privateKey(currentPrivateKeyId)
                .orElseThrow(() -> new RuntimeException("No current private key found (?)"));
        var response = new TokenResponse(
                JWS.jwsWrap(JWA.RS256, accessToken, currentPrivateKeyId, currentPrivateKey),
                JWS.jwsWrap(JWA.RS256, refreshToken, currentPrivateKeyId, currentPrivateKey),
                JWS.jwsWrap(JWA.RS256, idToken, currentPrivateKeyId, currentPrivateKey),
                Duration.ofMinutes(5L),
                codeData.get().scopes()
        );


        return new JsonResponse(response);

    }


}
