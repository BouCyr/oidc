package app.cbo.oidc.java.server.endpoints.authenticate;

import app.cbo.oidc.java.server.backends.OngoingAuths;
import app.cbo.oidc.java.server.backends.Sessions;
import app.cbo.oidc.java.server.backends.Users;
import app.cbo.oidc.java.server.credentials.PasswordEncoder;
import app.cbo.oidc.java.server.credentials.TOTP;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.endpoints.AuthError;
import app.cbo.oidc.java.server.endpoints.HTMLInteraction;
import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.endpoints.RedirectInteraction;
import app.cbo.oidc.java.server.endpoints.authorize.AuthentSuccessful;
import app.cbo.oidc.java.server.utils.Utils;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static app.cbo.oidc.java.server.credentials.AuthenticationMode.*;

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
            SessionId sessionIdOptional,

            Map<String, Collection<String>> rawParams) throws AuthError {

        AuthenticateEndpointParams params = new AuthenticateEndpointParams(rawParams);


        if(Utils.isBlank(params.login())) {

            try {
                return new HTMLInteraction(
                        getClass().getClassLoader().getResourceAsStream("login.html"),
                        Map.of("__ONGOING__", params.ongoing()));
            }catch(IOException e){
                throw new AuthError(AuthError.Code.server_error, "Unable to process logni template");
            }
        }else{

            var authentications = EnumSet.of(DECLARATIVE);
            int acrLevel = 0;

            var user = Users.getInstance().find(params::login)
                    .orElseThrow(() -> new AuthError(AuthError.Code.access_denied, "Invalid username"));

            authentications.add(USER_FOUND);

            if(!Utils.isBlank(params.password())){
                if( PasswordEncoder.confront(params.password(), user.pwd())) {
                    authentications.add(PASSWORD_OK);
                }else{
                    //TODO [31/03/2023] same msg for username not found & wrong password
                    throw new AuthError(AuthError.Code.access_denied, "wrong password username");
                }
            }

            if(!Utils.isBlank(params.totp())){
                if(TOTP.confront (params.totp(), user.totpKey())){
                    authentications.add(TOTP_OK);
                }else{
                    //TODO [31/03/2023] same msg for username not found & wrgon password
                    throw new AuthError(AuthError.Code.access_denied, "invalid TOTP");
                }
            }


            var sessionId = Sessions.getInstance().createSession(user, authentications);
            var originalAuthorizeParams = OngoingAuths.getInstance().retrieve(params.ongoing());
            return new AuthentSuccessful(sessionId, originalAuthorizeParams.orElseThrow(()->new AuthError(AuthError.Code.server_error, "Unable to retrieve the original auhtorization request")));







        }



    }
}
