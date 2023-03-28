package app.cbo.oidc.java.server.endpoints.authorize;

import app.cbo.oidc.java.server.backends.CodesDB;
import app.cbo.oidc.java.server.backends.SessionsDB;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.endpoints.AuthError;
import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.endpoints.RedirectInteraction;
import app.cbo.oidc.java.server.oidc.OIDCFlow;
import app.cbo.oidc.java.server.oidc.OIDCPromptValues;
import app.cbo.oidc.java.server.datastored.User;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

//TODO [20/03/2023] is the front end OIDC endpoint
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

    public Interaction treatRequest(
            Optional<SessionId> sessionIdOptional,
            Map<String, Collection<String>> rawParams) throws AuthError {


        //put params in the dedicated record
        AuthorizeEndpointParams params = new AuthorizeEndpointParams(rawParams);


        //3.1.2.2.  Authentication Request Validation
        //The Authorization Server MUST validate all the OAuth 2.0 parameters according to the OAuth 2.0 specification.
        //check their validity
        AuthorizeEndpointParams.checkParams(params);
        LOGGER.info("Request params are valid");
        //deduce the requested flow from responsetypes
        OIDCFlow flow = OIDCFlow.fromResponseType(params.responseTypes(), params);
        LOGGER.info("Selected OIDC flow is "+flow.name());

        //additional checks for specific flows
        AuthorizeEndpointParams.checkParamsForFlow(params, flow);
        LOGGER.info("Request params are valid for flow "+flow.name() );

        //3.1.2.3.  Authorization Server Authenticates End-User
        return checkIfAuthenticated(sessionIdOptional, flow, params);


    }

    private Interaction checkIfAuthenticated(Optional<SessionId> sessionIdOptional,
                                             OIDCFlow flow,
                                             AuthorizeEndpointParams params) throws AuthError {

        LOGGER.info("Checking if user already has a session");
        Optional<Session> userSession;
        if(sessionIdOptional.isEmpty()){
            //no session cookie, user is not authenticated
            userSession = Optional.empty();
        } else {
            userSession = SessionsDB.getInstance().getSession(sessionIdOptional.get());
        }

        if(userSession.isEmpty() && params.prompt().contains(OIDCPromptValues.NONE)){
            LOGGER.info("User has no session and client required no interaction. Sending back with error "+AuthError.Code.access_denied);
            throw new AuthError(AuthError.Code.access_denied, "Requested no interaction with no authenticated user", params);
        }
        if(userSession.isEmpty() || params.prompt().contains(OIDCPromptValues.LOGIN)){
            LOGGER.info("User has no session or client required reuthentication. Redirect to login page");
            return RedirectInteraction.internal("/login", params, Collections.emptyMap() );
        }

        var session = userSession.get(); //user has a vliad sess
        //TODO [20/03/2023] check params.maxAge
        //TODO [20/03/2023] check ACR - if current authent has acr < requested, a new authentication should be done with the "updated" acr value check

        LOGGER.info("User has a valid, active session matching the client requirements");
        return this.checkConsent(flow, userSession.get().user(), params);
    }


    private Interaction checkConsent(OIDCFlow flow, User user, AuthorizeEndpointParams params) {

        var notYetConsentedTo = params.scope()
                .stream()
                .filter(scope -> !user.hasConsentedTo(params.clientId().orElse("..."), scope)) //TODO [20/03/2023] handle (...) in User
                .collect(Collectors.toSet());
        
        if(notYetConsentedTo.isEmpty()) {
            LOGGER.info("User has already consented to all requested scopes");
            return this.authSuccess(flow, user, params);
        } else {
            LOGGER.info("Client is requesting scopes the user has not yet consented to transmit.");
            return RedirectInteraction.internal("/consent", params, Map.of("scopes", String.join(" ", notYetConsentedTo)));
        }



    }

    private Interaction authSuccess(OIDCFlow flow, User user, AuthorizeEndpointParams originalParams) {

        //cf 3.1.2.5
        // HTTP/1.1 302 Found  Location: https://client.example.org/cb?code=SplxlOBeZQQYbYS6WxSbIA&state=af0ifjsldkj

        //the Authorization Response MUST return the parameters defined in Section 4.1.2 of OAuth 2.0

        if(originalParams.redirectUri().isEmpty()){
            //should not happens here, but...
            return new AuthError(AuthError.Code.invalid_request, "Missing redirect_uri");
        }

        String code = CodesDB.getInstance().createFor(user);
        Map<String, String> params = new HashMap<>();
        params.put("code", code);
        if(originalParams.state().isPresent()){
            params.put("state", originalParams.state().get());
        }

        LOGGER.info("Authorization OK for flow "+flow.name()+". Redirecting the user to redirect uri with the code");
        return RedirectInteraction.external(originalParams.redirectUri().get(), params);
    }




}
