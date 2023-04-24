package app.cbo.oidc.java.server.endpoints.token;

import app.cbo.oidc.java.server.backends.Codes;
import app.cbo.oidc.java.server.backends.KeySet;
import app.cbo.oidc.java.server.backends.Sessions;
import app.cbo.oidc.java.server.backends.Users;
import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Code;
import app.cbo.oidc.java.server.endpoints.AuthErrorInteraction;
import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;
import app.cbo.oidc.java.server.jwt.JWA;
import app.cbo.oidc.java.server.jwt.JWS;
import app.cbo.oidc.java.server.oidc.tokens.AccessOrRefreshToken;
import app.cbo.oidc.java.server.oidc.tokens.IdToken;
import app.cbo.oidc.java.server.utils.HttpCode;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

public class TokenEndpoint {

    private final static Logger LOGGER = Logger.getLogger(TokenEndpoint.class.getCanonicalName());

    private static TokenEndpoint instance = null;

    private TokenEndpoint() {
    }

    public static TokenEndpoint getInstance() {
        if (instance == null) {
            instance = new TokenEndpoint();
        }
        return instance;
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


        //Are the client credentials OK ? (none would be OK for the moment)
        if (!this.authenticateClient(authClientId, clientSecret)) {
            return new JsonError(HttpCode.BAD_REQUEST, AuthErrorInteraction.Code.access_denied.name());
        }

        //if client id is in both params and authorization header, do they match ?
        if (authClientId != null && params.clientId() != null && !authClientId.equals(params.clientId())) {
            return new JsonError(HttpCode.BAD_REQUEST, AuthErrorInteraction.Code.invalid_request.name());
        }

        //have we at least one clientId somewhere ?
        if (authClientId == null && params.clientId() == null) {
            return new JsonError(HttpCode.BAD_REQUEST, AuthErrorInteraction.Code.invalid_request.name());
        }

        //the clientId may be found in credentials OR in the params.
        //we already check that we have at least one, and if two that they match
        var clientId = ClientId.of(authClientId != null ? authClientId : params.clientId());


        var codeData = Codes.getInstance().consume(Code.of(params.code()), clientId, URLDecoder.decode(params.redirectUri(), StandardCharsets.UTF_8))
                .orElseThrow(() -> new JsonError(HttpCode.BAD_REQUEST, AuthErrorInteraction.Code.invalid_grant.name()));


        var user = Users.getInstance().find(codeData.userId())
                .orElseThrow(() -> new JsonError(HttpCode.BAD_REQUEST, AuthErrorInteraction.Code.invalid_grant.name()));


        var session = Sessions.getInstance().find(codeData.sessionId())
                .orElseThrow(() -> new JsonError(HttpCode.BAD_REQUEST, AuthErrorInteraction.Code.invalid_grant.name()));


        var idToken = new IdToken(
                user.sub(),
                "ME",//TODO [14/04/2023]
                List.of(clientId.getClientId()),
                LocalDateTime.now().plusMinutes(5L).toEpochSecond(ZoneOffset.UTC), //TODO [14/04/2023]
                LocalDateTime.now().toEpochSecond(ZoneOffset.UTC),
                session.authTime().toEpochSecond(ZoneOffset.UTC),
                Optional.empty(), //TODO [14/04/2023]
                "level" + session.authentications().size(),
                session.authentications().stream().map(Enum::name).toList(),
                Optional.of(clientId.getClientId()));

        //access and refresh tokens will be transmitted as JWS, so we do not have to store them
        //any token received will be valid if signature is OK.
        var accessToken = new AccessOrRefreshToken(
                user.sub(),
                LocalDateTime.now().plusMinutes(5L).toEpochSecond(ZoneOffset.UTC),
                codeData.scopes());
        var refreshToken = new AccessOrRefreshToken(
                user.sub(),
                LocalDateTime.now().plusMinutes(5L).toEpochSecond(ZoneOffset.UTC),
                codeData.scopes());

        var currentPrivateKeyId = KeySet.getInstance().current();
        var currentPrivateKey = KeySet.getInstance().privateKey(currentPrivateKeyId)
                .orElseThrow(() -> new RuntimeException("No current private key found (?)"));
        var response = new TokenResponse(
                JWS.jwsWrap(JWA.RS256, accessToken, currentPrivateKeyId.getKeyId(), currentPrivateKey),
                JWS.jwsWrap(JWA.RS256, refreshToken, currentPrivateKeyId.getKeyId(), currentPrivateKey),
                JWS.jwsWrap(JWA.RS256, idToken, currentPrivateKeyId.getKeyId(), currentPrivateKey),
                Duration.ofMinutes(5L),
                codeData.scopes()
        );


        return new JsonResponse(response);

    }

    private boolean authenticateClient(String clientId, String clientSecret) {

        //td check if client MUST authenticate (ClientRegirstry to be created)

        if (clientId == null)
            return clientSecret == null;
        else
            return clientId.equals(clientSecret);//TODO [14/04/2023] something smarter
    }


}
