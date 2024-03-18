package app.cbo.oidc.java.server.http.authorize;

import app.cbo.oidc.java.server.backends.claims.ClaimsResolver;
import app.cbo.oidc.java.server.backends.codes.CodeSupplier;
import app.cbo.oidc.java.server.backends.keys.KeySet;
import app.cbo.oidc.java.server.backends.ongoingAuths.OngoingAuthsStorer;
import app.cbo.oidc.java.server.backends.users.UserFinder;
import app.cbo.oidc.java.server.credentials.AuthenticationLevel;
import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Code;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.datastored.user.User;
import app.cbo.oidc.java.server.http.AuthErrorInteraction;
import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jwt.JWA;
import app.cbo.oidc.java.server.jwt.JWS;
import app.cbo.oidc.java.server.oidc.Issuer;
import app.cbo.oidc.java.server.oidc.OIDCFlow;
import app.cbo.oidc.java.server.oidc.OIDCPromptValues;
import app.cbo.oidc.java.server.oidc.tokens.AccessOrRefreshToken;
import app.cbo.oidc.java.server.oidc.tokens.IdToken;
import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.utils.Utils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Injectable
public class AuthorizeEndpoint {


    private final Issuer myself;
    private final OngoingAuthsStorer ongoingAuthsStorer;
    private final UserFinder userFinder;
    private final CodeSupplier codeSupplier;
    private final KeySet keySet;
    private final ClaimsResolver claimsResolver;


    public AuthorizeEndpoint(
            Issuer myself,
            OngoingAuthsStorer ongoingAuthsStorer,
            UserFinder userFinder,
            CodeSupplier codeSupplier,
            KeySet keySet,
            ClaimsResolver claimsResolver) {
        this.myself = myself;
        this.ongoingAuthsStorer = ongoingAuthsStorer;
        this.userFinder = userFinder;
        this.codeSupplier = codeSupplier;
        this.keySet = keySet;
        this.claimsResolver = claimsResolver;
    }


    private final static Logger LOGGER = Logger.getLogger(AuthorizeEndpoint.class.getCanonicalName());

    @NotNull
    public Interaction treatRequest(
            @NotNull Optional<Session> session,
            @NotNull Map<String, Collection<String>> rawParams) throws AuthErrorInteraction {


        //put params in the dedicated record
        AuthorizeParams params = new AuthorizeParams(rawParams);


        //3.1.2.2.  Authentication Request Validation
        //The Authorization Server MUST validate all the OAuth 2.0 parameters according to the OAuth 2.0 specification.
        //check their validity
        AuthorizeParams.checkParams(params);
        LOGGER.info("Request params are valid");
        //deduce the requested flow from response types
        OIDCFlow flow = OIDCFlow.fromResponseType(params.responseTypes(), params);
        LOGGER.info("Selected OIDC flow is "+flow.name());

        //additional checks for specific flows
        AuthorizeParams.checkParamsForFlow(params, flow);
        LOGGER.info("Request params are valid for flow "+flow.name() );

        //3.1.2.3.  Authorization Server Authenticates End-User
        return checkIfAuthenticated(session, flow, params);


    }

    private Interaction checkIfAuthenticated(Optional<Session> userSession,
                                             OIDCFlow flow,
                                             AuthorizeParams params) throws AuthErrorInteraction {

        LOGGER.info("Checking if userId already has a (valid) session");
        if(userSession.isEmpty() && params.prompt().contains(OIDCPromptValues.NONE)){
            LOGGER.info("User has no session and client required no interaction. Sending back with error "+ AuthErrorInteraction.Code.access_denied);
            throw new AuthErrorInteraction(AuthErrorInteraction.Code.access_denied, "Requested no interaction with no authenticated userId", params);
        }
        if(userSession.isEmpty() || params.prompt().contains(OIDCPromptValues.LOGIN)){
            LOGGER.info("User has no session or client required new authentication. Redirect to login page");
            return new RedirectToLoginInteraction(ongoingAuthsStorer.store(params));
        }

        var session = userSession.get(); //userId has a valid session
        var user = this.userFinder.find(session.userId())
                //user may have been deleted since the session was created...
                .orElseThrow(() -> new AuthErrorInteraction(AuthErrorInteraction.Code.server_error, "Unable to find user linked to session"));


        if(params.maxAge().isPresent()){
            final long maxAge;
            try{
                maxAge = Long.parseLong(params.maxAge().get());
            }catch(NumberFormatException e){
                throw new AuthErrorInteraction(AuthErrorInteraction.Code.invalid_request, "max_age should be parsable as a long");
            }
            var sessionAge = Duration.between(session.authTime(), LocalDateTime.now());
            if (sessionAge.toSeconds() > maxAge) {
                return new RedirectToLoginInteraction(ongoingAuthsStorer.store(params));
            }
        }

        //[03/04/2023] OIDC specs is not very clear...
        if (!Utils.isEmpty(params.acrValues())) {

            //from what I understand, the client gives the accepted acr in acr_values.
            //if any of the acr values of acr_values is compatible with the acr of the session, we are good


            //in our case, acr supported by the system takes the form of 'levelX', where X will be the number of authentication methods used when authenticating
            //so if any acr in acr_values has X below session.acr, we are good
            //so if the min acr of acr_values is below session.acr, we are good!

            var minimumAcr = params.acrValues().stream().map(AuthenticationLevel::fromAcr)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .min(Comparator.comparingInt(AuthenticationLevel::level));

            var sessionAcr = new AuthenticationLevel(session.authentications());

            //the least secured acr accepted is above the current session ACR. Must reauthenticate
            if (minimumAcr.isPresent() && minimumAcr.get().level() > sessionAcr.level()) {
                LOGGER.info("client requested an acr of at least " + minimumAcr.get().level() + ", current session acr is " + sessionAcr.level());
                LOGGER.info("User must use additional authentication factors");
                //TODO [27/04/2023] We should tell the login form the required acr / number of amr
                return new RedirectToLoginInteraction(ongoingAuthsStorer.store(params));
            }
        }

        LOGGER.info("User has a valid, active session matching the client requirements");
        return this.checkConsent(flow, user, params, session);
    }


    private Interaction checkConsent(OIDCFlow flow, User user, AuthorizeParams params, Session session) throws AuthErrorInteraction {

        var notYetConsentedTo = params.scopes()
                .stream()
                .filter(scope -> !user.hasConsentedTo(params.clientId().orElse("..."), scope)) //TODO [20/03/2023] handle orElse(...) in User
                .collect(Collectors.toSet());

        if (notYetConsentedTo.isEmpty()) {
            LOGGER.info("User has already consented to all requested scopes");
            return this.authSuccess(flow, user, params, session);
        } else {
            LOGGER.info("Client is requesting scopes the userId has not yet consented to transmit.");
            return new RedirectToConsentInteraction(ongoingAuthsStorer.store(params),
                    params.clientId().orElseThrow(() -> new AuthErrorInteraction(AuthErrorInteraction.Code.invalid_request, "ClientId si required")), params.scopes());
        }


    }

    private Interaction authSuccess(OIDCFlow flow, User user, AuthorizeParams originalParams, Session session) {

        // cf 3.1.2.5
        // HTTP/1.1 302 Found  Location: https://client.example.org/cb?code=SplxlOBeZQQYbYS6WxSbIA&state=af0ifjsldkj
        // the Authorization Response MUST return the parameters defined in Section 4.1.2 of OAuth 2.0

        if (originalParams.redirectUri().isEmpty()) {
            //should not happens here, but...
            return new AuthErrorInteraction(AuthErrorInteraction.Code.invalid_request, "Missing redirect_uri");
        }
        if (originalParams.clientId().isEmpty()) {
            //should not happens here, but...
            return new AuthErrorInteraction(AuthErrorInteraction.Code.invalid_request, "Missing clientid");
        }


        LOGGER.info("Authorization OK for flow " + flow.name() + ". Redirecting the user to redirect uri with the code");
        if (flow == OIDCFlow.AUTHORIZATION) {
            return authorizationFlowSuccess(user, originalParams, session);
        } else if (flow == OIDCFlow.IMPLICIT) {
            return implicitFlowSuccess(user, originalParams, session);
        } else {
            throw new IllegalArgumentException("Hybrid flow not implemented");
        }
    }

    private AuthorizationFlowSuccessInteraction authorizationFlowSuccess(User user, AuthorizeParams originalParams, Session session) {
        Code authCode = this.codeSupplier.createFor(
                user.getUserId(),
                ClientId.of(originalParams.clientId().get()),
                SessionId.of(session.id()),
                originalParams.redirectUri().get(),
                originalParams.scopes(),
                originalParams.nonce().orElse(null));

        return new AuthorizationFlowSuccessInteraction(originalParams, authCode);
    }

    private ImplicitFlowSuccessInteraction implicitFlowSuccess(User user, AuthorizeParams originalParams, Session session) {
        var clock = Clock.systemUTC();
        //TODO [26/05/2023] extract idtoken generation, clock handling and keyset mgt in a dedicated service (done twice here & code endpoint)
        var idToken = new IdToken(
                user.sub(),
                this.myself.getIssuerId(),
                List.of(originalParams.clientId().get()),
                Instant.now(clock).plus(Duration.ofMinutes(5L)).getEpochSecond(),
                Instant.now(clock).getEpochSecond(),
                session.authTime().toEpochSecond(ZoneOffset.UTC),
                originalParams.nonce(),
                new AuthenticationLevel(session.authentications()).name(),
                session.authentications().stream().map(Enum::name).toList(),
                originalParams.clientId(),
                new HashMap<>());

        var currentPrivateKeyId = this.keySet.current();
        var currentPrivateKey = this.keySet.privateKey(currentPrivateKeyId)
                .orElseThrow(() -> new RuntimeException("No current private key found (?)"));


        boolean withAccessToken = originalParams.responseTypes().contains("token");
        if (withAccessToken) {
            var ttl = Duration.ofMinutes(5L);
            var accessToken = new AccessOrRefreshToken(
                    this.myself.getIssuerId(),
                    AccessOrRefreshToken.TYPE_ACCESS,
                    user.sub(),
                    Instant.now(clock).plus(ttl).getEpochSecond(),
                    originalParams.scopes());
            var atWrapped = JWS.jwsWrap(JWA.RS256, accessToken, currentPrivateKeyId, currentPrivateKey);

            LOGGER.info("Implicit flow with access_token ; claims are NOT added to the id_token, and should be retrieved from userInfo endpoint using the provided access_token");
            var itWrapped = JWS.jwsWrap(JWA.RS256, idToken, currentPrivateKeyId, currentPrivateKey);
            LOGGER.info("Implicit flow success ; Returning id_token and access_token");
            return ImplicitFlowSuccessInteraction.withAccessToken(originalParams, itWrapped, atWrapped, ttl);
        } else {
            // OIDC core 5.4
            // The Claims requested by the profile, email, address, and phone scope values are returned from the UserInfo Endpoint,
            // as described in Section 5.3.2, when a response_type value is used that results in an Access Token being issued. However, when no Access Token is issued
            // (which is the case for the response_type value id_token), the resulting Claims are returned in the ID Token.

            LOGGER.info("Implicit flow without access_token ; claims are added to the id_token");
            var claims = this.claimsResolver.claimsFor(user.getUserId(), Set.copyOf(originalParams.scopes()));
            claims.forEach((claim, val) -> idToken.extranodes().put(claim, val));

            var itWrapped = JWS.jwsWrap(JWA.RS256, idToken, currentPrivateKeyId, currentPrivateKey);
            LOGGER.info("Implicit flow success ; Returning id_token ");
            return ImplicitFlowSuccessInteraction.withoutAccessToken(originalParams, itWrapped);
        }
    }


}
