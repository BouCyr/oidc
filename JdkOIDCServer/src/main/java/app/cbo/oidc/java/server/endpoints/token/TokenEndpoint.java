package app.cbo.oidc.java.server.endpoints.token;

import app.cbo.oidc.java.server.backends.codes.CodeConsumer;
import app.cbo.oidc.java.server.backends.keys.KeySet;
import app.cbo.oidc.java.server.backends.sessions.SessionFinder;
import app.cbo.oidc.java.server.backends.users.UserFinder;
import app.cbo.oidc.java.server.credentials.AuthenticationLevel;
import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Code;
import app.cbo.oidc.java.server.endpoints.AuthErrorInteraction;
import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.json.JSON;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;
import app.cbo.oidc.java.server.jwt.JWA;
import app.cbo.oidc.java.server.jwt.JWS;
import app.cbo.oidc.java.server.oidc.tokens.AccessOrRefreshToken;
import app.cbo.oidc.java.server.oidc.tokens.IdToken;
import app.cbo.oidc.java.server.utils.HttpCode;

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

public class TokenEndpoint {

    private final static Logger LOGGER = Logger.getLogger(TokenEndpoint.class.getCanonicalName());

    private final CodeConsumer codeConsumer;
    private final UserFinder userFinder;
    private final SessionFinder sessionFinder;
    private final KeySet keySet;

    public TokenEndpoint(CodeConsumer codeConsumer, UserFinder userFinder, SessionFinder sessionFinder, KeySet keySet) {
        this.codeConsumer = codeConsumer;
        this.userFinder = userFinder;
        this.sessionFinder = sessionFinder;
        this.keySet = keySet;
    }

    @NotNull
    public Interaction treatRequest(@NotNull TokenParams params, @Nullable String authClientId, @Nullable String clientSecret) throws JsonError {
        /*
        The Authorization Server MUST validate the Token Request as follows:

        Authenticate the Client if it was issued Client Credentials or if it uses another Client Authentication method, per Section 9.
        Ensure the Authorization Code was issued to the authenticated Client.
        Verify that the Authorization Code is valid.
                If possible, verify that the Authorization Code has not been previously used.
                Ensure that the redirect_uri parameter value is identical to the redirect_uri parameter value that was included in the initial Authorization Request. If the redirect_uri parameter value is not present when there is only one registered redirect_uri value, the Authorization Server MAY return an error (since the Client should have included the parameter) or MAY proceed without an error (since OAuth 2.0 permits the parameter to be omitted in this case).
        Verify that the Authorization Code used was issued in response to an OpenID Connect Authentication Request (so that an ID Token will be returned from the Token Endpoint).
        */

        LOGGER.info(("'" + (authClientId != null ? authClientId : "?") + "' tries to consume a code"));

        //Are the client credentials OK ? (none would be OK for the moment)
        if (!this.authenticateClient(authClientId, clientSecret)) {
            LOGGER.info("Invalid client credentials");
            return new JsonError(HttpCode.BAD_REQUEST, AuthErrorInteraction.Code.access_denied.name());
        }
        LOGGER.info("Client is authenticated");

        //if client id is in both params and authorization header, do they match ?
        if (authClientId != null && params.clientId() != null && !authClientId.equals(params.clientId())) {
            LOGGER.info("Client id does not match the client  the code was generated for");
            return new JsonError(HttpCode.BAD_REQUEST, AuthErrorInteraction.Code.invalid_request.name());
        }

        //have we at least one clientId somewhere ?
        if (authClientId == null && params.clientId() == null) {
            LOGGER.info("Client id not present");
            return new JsonError(HttpCode.BAD_REQUEST, "clientid not present");
        }

        //the clientId may be found in credentials OR in the params.
        //we already check that we have at least one, and if two that they match
        var clientId = ClientId.of(authClientId != null ? authClientId : params.clientId());


        if (params.redirectUri() == null) {
            throw new JsonError(HttpCode.BAD_REQUEST, "redirecturi not present");
        }
        if (params.grantType() == null) {
            throw new JsonError(HttpCode.BAD_REQUEST, "grantype not present");
        }


        var codeData = this.codeConsumer.consume(Code.of(params.code()), clientId, URLDecoder.decode(params.redirectUri(), StandardCharsets.UTF_8))
                .orElseThrow(() -> new JsonError(HttpCode.BAD_REQUEST, AuthErrorInteraction.Code.invalid_grant.name()));
        LOGGER.info("Code is retrieved");

        var user = this.userFinder.find(codeData.userId())
                .orElseThrow(() -> new JsonError(HttpCode.BAD_REQUEST, AuthErrorInteraction.Code.invalid_grant.name()));
        LOGGER.info("User is found");

        var session = this.sessionFinder.find(codeData.sessionId())
                .orElseThrow(() -> new JsonError(HttpCode.BAD_REQUEST, AuthErrorInteraction.Code.invalid_grant.name()));
        LOGGER.info("User session the code was generated from is found");

        var clock = Clock.systemUTC();

        var idToken = new IdToken(
                user.sub(),
                "http://localhost:4951",
                List.of(clientId.getClientId()),
                Instant.now(clock).plus(Duration.ofMinutes(5L)).getEpochSecond(),
                Instant.now(clock).getEpochSecond(),
                session.authTime().toEpochSecond(ZoneOffset.UTC),
                Optional.of(codeData.nonce()),
                new AuthenticationLevel(session.authentications()).name(),
                session.authentications().stream().map(Enum::name).toList(),
                Optional.of(clientId.getClientId()),
                new HashMap<>());


        idToken.extranodes().put("at_hash", "rooooo"); //TODO [25/04/2023] at_hash management
        LOGGER.info("idToken is : " + JSON.jsonify(idToken));

        //access and refresh tokens will be transmitted as JWS, so we do not have to store them
        //any token received will be valid if signature is OK.
        var accessToken = new AccessOrRefreshToken(
                AccessOrRefreshToken.TYPE_ACCESS,
                user.sub(),
                Instant.now(clock).plus(Duration.ofMinutes(5L)).getEpochSecond(),
                codeData.scopes());
        var refreshToken = new AccessOrRefreshToken(
                AccessOrRefreshToken.TYPE_REFRESH,
                user.sub(),
                Instant.now(clock).plus(Duration.ofMinutes(5L)).getEpochSecond(),
                codeData.scopes());

        var currentPrivateKeyId = this.keySet.current();
        var currentPrivateKey = this.keySet.privateKey(currentPrivateKeyId)
                .orElseThrow(() -> new RuntimeException("No current private key found (?)"));
        var response = new TokenResponse(
                JWS.jwsWrap(JWA.RS256, accessToken, currentPrivateKeyId, currentPrivateKey),
                JWS.jwsWrap(JWA.RS256, refreshToken, currentPrivateKeyId, currentPrivateKey),
                JWS.jwsWrap(JWA.RS256, idToken, currentPrivateKeyId, currentPrivateKey),
                Duration.ofMinutes(5L),
                codeData.scopes()
        );


        return new JsonResponse(response);

    }

    private boolean authenticateClient(String clientId, String clientSecret) {

        //td check if client MUST authenticate (ClientRegistry to be created)

        if (clientId == null)
            return clientSecret == null;
        else
            return clientId.equals(clientSecret);//TODO [14/04/2023] client registry ?
    }


}
