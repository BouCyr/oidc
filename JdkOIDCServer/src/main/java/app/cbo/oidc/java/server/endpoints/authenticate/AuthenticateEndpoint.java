package app.cbo.oidc.java.server.endpoints.authenticate;

import app.cbo.oidc.java.server.backends.OngoingAuths;
import app.cbo.oidc.java.server.backends.Sessions;
import app.cbo.oidc.java.server.backends.Users;
import app.cbo.oidc.java.server.credentials.PasswordEncoder;
import app.cbo.oidc.java.server.credentials.TOTP;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.endpoints.AuthErrorInteraction;
import app.cbo.oidc.java.server.endpoints.HTMLInteraction;
import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
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

    @NotNull public Interaction treatRequest(
            @NotNull Optional<Session> currentSession, //TODO [05/04/2023]
            @NotNull Map<String, Collection<String>> rawParams) throws AuthErrorInteraction {

        AuthenticateParams params = new AuthenticateParams(rawParams);


        if(Utils.isBlank(params.login())) {

            try {
                return new DisplayLoginFormInteraction(
                        getClass().getClassLoader().getResourceAsStream("login.html"),
                        Map.of("__ONGOING__", params.ongoing()));
            }catch(IOException e){
                throw new AuthErrorInteraction(AuthErrorInteraction.Code.server_error, "Unable to process logni template");
            }
        }else{

            var authentications = EnumSet.of(DECLARATIVE);
            int acrLevel = 0;

            var user = Users.getInstance().find(params::login)
                    .orElseThrow(() -> new AuthErrorInteraction(AuthErrorInteraction.Code.access_denied, "Invalid credentials"));

            authentications.add(USER_FOUND);

            if(!Utils.isBlank(params.password())){
                if( PasswordEncoder.getInstance().confront(params.password(), user.pwd())) {
                    authentications.add(PASSWORD_OK);
                }else{
                    throw new AuthErrorInteraction(AuthErrorInteraction.Code.access_denied, "Invalid credentials");
                }
            }

            if(!Utils.isBlank(params.totp())){
                if(TOTP.confront (params.totp(), user.totpKey())){
                    authentications.add(TOTP_OK);
                }else{
                    throw new AuthErrorInteraction(AuthErrorInteraction.Code.access_denied, "Invalid credentials");
                }
            }

            var sessionId = Sessions.getInstance().createSession(user, authentications);
            var originalAuthorizeParams = OngoingAuths.getInstance().retrieve(params::ongoing);
            return new AuthentSuccessfulInteraction(sessionId, originalAuthorizeParams.orElseThrow(()->new AuthErrorInteraction(AuthErrorInteraction.Code.server_error, "Unable to retrieve the original auhtorization request")));

        }



    }
}
