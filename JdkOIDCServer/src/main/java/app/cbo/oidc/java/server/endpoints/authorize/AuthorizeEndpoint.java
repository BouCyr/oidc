package app.cbo.oidc.java.server.endpoints.authorize;

import app.cbo.oidc.java.server.backends.Codes;
import app.cbo.oidc.java.server.backends.Users;
import app.cbo.oidc.java.server.datastored.*;
import app.cbo.oidc.java.server.endpoints.AuthErrorInteraction;
import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.endpoints.RedirectInteraction;
import app.cbo.oidc.java.server.endpoints.consent.ConsentHandler;
import app.cbo.oidc.java.server.endpoints.consent.ConsentParams;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.oidc.OIDCFlow;
import app.cbo.oidc.java.server.oidc.OIDCPromptValues;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class AuthorizeEndpoint {

    private static AuthorizeEndpoint instance = null;
    private AuthorizeEndpoint(){ }
    public static AuthorizeEndpoint getInstance() {
        if(instance == null){
          instance = new AuthorizeEndpoint();
        }
        return instance;
    }
    
    
    
    private final static Logger LOGGER = Logger.getLogger(AuthorizeEndpoint.class.getCanonicalName());

    @NotNull public Interaction treatRequest(
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
            return new RedirectToLoginInteraction(params);
        }

        var session = userSession.get(); //userId has a valid session
        var user = Users.getInstance().find(session.userId())
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
            if(sessionAge.toSeconds() > maxAge ){
                return new RedirectToLoginInteraction(params);
            }
        }

        //TODO [20/03/2023] check ACR - if current authentication has acr < requested, a new authentication should be done with the "updated" acr value check
        //[03/04/2023] OIDC specs is not very clear...

        LOGGER.info("User has a valid, active session matching the client requirements");
        return this.checkConsent(flow, user, params, session);
    }


    private Interaction checkConsent(OIDCFlow flow, User user, AuthorizeParams params, Session session) {

        var notYetConsentedTo = params.scopes()
                .stream()
                .filter(scope -> !user.hasConsentedTo(params.clientId().orElse("..."), scope)) //TODO [20/03/2023] handle (...) in User
                .collect(Collectors.toSet());

        if (notYetConsentedTo.isEmpty()) {
            LOGGER.info("User has already consented to all requested scopes");
            return this.authSuccess(flow, user, params, session);
        } else {
            LOGGER.info("Client is requesting scopes the userId has not yet consented to transmit.");
            return RedirectInteraction.internal(
                    ConsentHandler.CONSENT_ENDPOINT,
                    params,
                    Map.of(
                            ConsentParams.SCOPES_REQUESTED, String.join(" ", notYetConsentedTo),
                            ConsentParams.CLIENT_ID, params.clientId().get()));
        }



    }

    private Interaction authSuccess(OIDCFlow flow, User user, AuthorizeParams originalParams, Session session) {

        //cf 3.1.2.5
        // HTTP/1.1 302 Found  Location: https://client.example.org/cb?code=SplxlOBeZQQYbYS6WxSbIA&state=af0ifjsldkj

        //the Authorization Response MUST return the parameters defined in Section 4.1.2 of OAuth 2.0

        if (originalParams.redirectUri().isEmpty()) {
            //should not happens here, but...
            return new AuthErrorInteraction(AuthErrorInteraction.Code.invalid_request, "Missing redirect_uri");
        }

        /*
            Code createFor(@NotNull UserId userId,
                   @NotNull ClientId clientId,
                   @NotNull SessionId sessionId,
                   @NotNull String redirectUri,
                   @NotNull List<String> scopes) {
         */
        Code authCode = Codes.getInstance().createFor(
                user.getUserId(),
                ClientId.of(originalParams.clientId().orElse("")),
                SessionId.of(session.id()),
                originalParams.redirectUri().orElse(""),
                originalParams.scopes(),
                originalParams.nonce().orElse(null));
        Map<String, String> params = new HashMap<>();
        params.put("code", authCode.getCode());
        if (originalParams.state().isPresent()) {
            params.put("state", originalParams.state().get());
        }

        LOGGER.info("Authorization OK for flow " + flow.name() + ". Redirecting the userId to redirect uri with the code");
        return RedirectInteraction.external(originalParams.redirectUri().get(), params);
    }




}
