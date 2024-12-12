package app.cbo.oidc.java.server.http.token.endpoints;

import app.cbo.oidc.java.server.backends.clients.ClientAuthenticator;
import app.cbo.oidc.java.server.backends.codes.CodeConsumer;
import app.cbo.oidc.java.server.backends.keys.KeySet;
import app.cbo.oidc.java.server.backends.sessions.SessionFinder;
import app.cbo.oidc.java.server.backends.tokens.AccessTokenGenerator;
import app.cbo.oidc.java.server.backends.users.UserFinder;
import app.cbo.oidc.java.server.credentials.AuthenticationLevel;
import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.http.JsonResponse;
import app.cbo.oidc.java.server.http.token.IdTokenCustomizer;
import app.cbo.oidc.java.server.http.token.JsonError;
import app.cbo.oidc.java.server.http.token.TokenResponse;
import app.cbo.oidc.java.server.http.token.params.CodeToTokenParams;
import app.cbo.oidc.java.server.json.JSON;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;
import app.cbo.oidc.java.server.jwt.JWA;
import app.cbo.oidc.java.server.jwt.JWS;
import app.cbo.oidc.java.server.oidc.Issuer;
import app.cbo.oidc.java.server.oidc.tokens.IdToken;
import app.cbo.oidc.java.server.oidc.tokens.RefreshToken;
import app.cbo.oidc.java.server.scan.BuildWith;
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

import static app.cbo.oidc.java.server.http.token.JsonError.Cause.*;
import static app.cbo.oidc.java.server.oidc.Constants.GrantType;

@Injectable
public class CodeToTokensEndpointImpl implements CodeToTokensEndpoint {

    private final static Logger LOGGER = Logger.getLogger(CodeToTokensEndpointImpl.class.getCanonicalName());


    private final Issuer myself;
    private final CodeConsumer codeConsumer;
    private final UserFinder userFinder;
    private final SessionFinder sessionFinder;
    private final KeySet keySet;
    private final IdTokenCustomizer idTokenCustomizer;
    private final ClientAuthenticator clientAuthenticator;
    private final AccessTokenGenerator accessTokenGenerator;

    @BuildWith
    public CodeToTokensEndpointImpl(
            Issuer myself,
            CodeConsumer codeConsumer,
            UserFinder userFinder,
            SessionFinder sessionFinder,
            KeySet keySet,
            IdTokenCustomizer idTokenCustomizer,
            ClientAuthenticator clientAuthenticator,
            AccessTokenGenerator accessTokenGenerator) {
        this.myself = myself;
        this.codeConsumer = codeConsumer;
        this.userFinder = userFinder;
        this.sessionFinder = sessionFinder;
        this.keySet = keySet;
        this.idTokenCustomizer = idTokenCustomizer;
        this.clientAuthenticator = clientAuthenticator;
        this.accessTokenGenerator = accessTokenGenerator;

    }

    @NotNull
    @Override
    public Interaction treatRequest(@NotNull CodeToTokenParams params, @Nullable ClientId authClientId, @Nullable String clientSecret) {
        /*
        The Authorization Server MUST validate the Token Request as follows:

        Authenticate the Client if it was issued Client Credentials or if it uses another Client Authentication method, per Section 9.
        Ensure the Authorization Code was issued to the authenticated Client.
        Verify that the Authorization Code is valid.
        If possible, verify that the Authorization Code has not been previously used.
        Ensure that the redirect_uri parameter value is identical to the redirect_uri parameter value that was included in the initial Authorization Request. If the redirect_uri parameter value is not present when there is only one registered redirect_uri value, the Authorization Server MAY return an error (since the Client should have included the parameter) or MAY proceed without an error (since OAuth 2.0 permits the parameter to be omitted in this case).
        Verify that the Authorization Code used was issued in response to an OpenID Connect Authentication Request (so that an ID Token will be returned from the Token Endpoint).
        */

        if (authClientId != null) {
            LOGGER.info(("'" + (!Utils.isEmpty(authClientId.get()) ? authClientId : "?") + "' tries to consume a code"));
        }

        //Are the client credentials OK ? (none would be OK for the moment)
        if (!this.clientAuthenticator.authenticate(authClientId, clientSecret)) {
            LOGGER.info("Invalid client credentials");
            return new JsonError(invalid_client);
        }
        LOGGER.info("Client is authenticated");


        //have we at least one clientId somewhere ?
        if (Utils.isBlank(authClientId) && Utils.isEmpty(params.clientId())) {
            LOGGER.info("Client id not present");
            return new JsonError(invalid_client);
        }

        //the clientId may be found in credentials OR in the params.
        //we already check that we have at least one, and if two that they match
        var clientId = authClientId != null ? authClientId : params.clientId();


        if (Utils.isEmpty(params.grantType())) {
            LOGGER.warning("grantType was not present");
            return new JsonError(invalid_request, "grant type not present");
        }

        if (Utils.isEmpty(params.redirectUri())) {
            LOGGER.warning("RedirectUri was not present");
            return new JsonError(invalid_request, "redirecturi not present");
        }

        if (!params.grantType().equals(GrantType.AUTHORIZATION_CODE)) {
            LOGGER.warning("grantType was not authorization_code");
            // warning : this is not WHAT the cause 'invalid_grant' means !
            return new JsonError(invalid_request, "invalid grant type");
        }


        var codeData = this.codeConsumer.consume(params.code(), clientId, URLDecoder.decode(params.redirectUri(), StandardCharsets.UTF_8));
        if (codeData.isEmpty()) {
            LOGGER.warning("Code could not be retrieved");
            return new JsonError(invalid_grant);
        }

        LOGGER.info("Code is retrieved");

        var user = this.userFinder.find(codeData.get().userId());
        if (user.isEmpty()) {
            LOGGER.warning("User could not be retrieved");
            return new JsonError(invalid_grant);
        }
        LOGGER.info("User is found");

        var session = this.sessionFinder.find(codeData.get().sessionId());
        if (session.isEmpty()) {
            LOGGER.warning("User session could not be retrieved");
            return new JsonError(invalid_grant);
        }
        LOGGER.info("User session the code was generated from is found");


        var clock = Clock.systemUTC();

        //TODO [a118608][14/03/2024] Simplify : the now() and new hashMap() could be set by default
        var idToken = new IdToken(
                user.get().sub(),
                this.myself.getIssuerId(),
                List.of(clientId.id()),
                Instant.now(clock).plus(Duration.ofMinutes(5L)).getEpochSecond(),
                Instant.now(clock).getEpochSecond(),
                session.get().authTime().toEpochSecond(ZoneOffset.UTC),
                Optional.ofNullable(codeData.get().nonce()),
                new AuthenticationLevel(session.get().authentications()).name(),
                session.get().authentications().stream().map(Enum::name).toList(),
                Optional.of(clientId.id()),
                new HashMap<>());
        idToken.extranodes().put("at_hash", "rooooo"); //TODO [25/04/2023] at_hash management

        idToken = this.idTokenCustomizer.customize(idToken);

        LOGGER.info("idToken is : " + JSON.jsonify(idToken));

        //access and refresh tokens will be transmitted as JWS, so we do not have to store them
        //any token received will be valid if signature is OK.
        var accessToken = this.accessTokenGenerator.generate(
                clientId,
                session.get(),
                codeData.get().resource()
        );


        var refreshToken = new RefreshToken(
                this.myself.getIssuerId(),
                RefreshToken.TYPE_REFRESH,
                user.get().sub(),
                session.get().id(),
                Instant.now(clock).plus(Duration.ofMinutes(5L)).getEpochSecond(),
                codeData.get().scopes());

        var currentPrivateKeyId = this.keySet.current();
        var currentPrivateKey = this.keySet.privateKey(currentPrivateKeyId)
                .orElseThrow(() -> new RuntimeException("No current private key found (?)"));
        var response = new TokenResponse(
                accessToken,
                JWS.jwsWrap(JWA.RS256, refreshToken, currentPrivateKeyId, currentPrivateKey),
                JWS.jwsWrap(JWA.RS256, idToken, currentPrivateKeyId, currentPrivateKey),
                Duration.ofMinutes(5L),
                codeData.get().scopes()
        );


        return new JsonResponse(response);

    }


}
