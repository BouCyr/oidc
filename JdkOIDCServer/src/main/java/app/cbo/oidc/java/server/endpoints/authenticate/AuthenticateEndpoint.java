package app.cbo.oidc.java.server.endpoints.authenticate;

import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.endpoints.AuthError;
import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeEndpoint;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeEndpointParams;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

public class AuthenticateEndpoint {

   private static AuthenticateEndpoint instance = null;
   private AuthenticateEndpoint(){ }
   public static AuthenticateEndpoint getInstance() {
       if(instance == null){
         instance = new AuthenticateEndpoint();
       }
       return instance;
   }

    private final static Logger LOGGER = Logger.getLogger(AuthenticateEndpoint.class.getCanonicalName());

    public Interaction treatRequest(
            Optional<SessionId> sessionIdOptional,
            Map<String, Collection<String>> rawParams) throws AuthError {

        AuthenticateEndpointParams params = new AuthenticateEndpointParams(rawParams);

        return null;


    }
}
