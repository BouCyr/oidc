package app.cbo.oidc.java.server.endpoints.authenticate;

import app.cbo.oidc.java.server.backends.ongoingAuths.OngoingAuthsFinder;
import app.cbo.oidc.java.server.backends.sessions.SessionSupplier;
import app.cbo.oidc.java.server.backends.users.UserFinder;
import app.cbo.oidc.java.server.credentials.TOTP;
import app.cbo.oidc.java.server.credentials.pwds.PasswordChecker;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.endpoints.AuthErrorInteraction;
import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.Utils;

import java.io.IOException;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static app.cbo.oidc.java.server.credentials.AuthenticationMode.DECLARATIVE;
import static app.cbo.oidc.java.server.credentials.AuthenticationMode.PASSWORD_OK;
import static app.cbo.oidc.java.server.credentials.AuthenticationMode.TOTP_OK;
import static app.cbo.oidc.java.server.credentials.AuthenticationMode.USER_FOUND;

public class AuthenticateEndpoint {


    private final OngoingAuthsFinder ongoingAuthsFinder;
    private final UserFinder userFinder;
    private final SessionSupplier sessionSupplier;

    private final PasswordChecker passwordChecker;

    public AuthenticateEndpoint(
            OngoingAuthsFinder ongoingAuthsFinder,
            UserFinder userFinder,
            SessionSupplier sessionSupplier, PasswordChecker passwordChecker) {
        this.ongoingAuthsFinder = ongoingAuthsFinder;
        this.userFinder = userFinder;
        this.sessionSupplier = sessionSupplier;
        this.passwordChecker = passwordChecker;
    }


    private final static Logger LOGGER = Logger.getLogger(AuthenticateEndpoint.class.getCanonicalName());

    @NotNull
    public Interaction treatRequest(
            @NotNull Optional<Session> currentSession, //TODO [05/04/2023]
            @NotNull Map<String, Collection<String>> rawParams) throws AuthErrorInteraction {

        AuthenticateParams params = new AuthenticateParams(rawParams);


        if(Utils.isBlank(params.login())) {
            try {
                return new DisplayLoginFormInteraction(
                        getClass().getClassLoader().getResourceAsStream("login.html"),
                        Map.of("__ONGOING__", params.ongoing()));
            }catch(IOException e){
                throw new AuthErrorInteraction(AuthErrorInteraction.Code.server_error, "Unable to process login template");
            }
        }else{

            var authentications = EnumSet.of(DECLARATIVE);

            var user = this.userFinder.find(params::login)
                    .orElseThrow(() -> new AuthErrorInteraction(AuthErrorInteraction.Code.access_denied, "Invalid credentials"));

            authentications.add(USER_FOUND);

            if(!Utils.isBlank(params.password())){
                if( passwordChecker.confront(params.password(), user.pwd())) {
                    authentications.add(PASSWORD_OK);
                }else{
                    //TODO [24/05/2023] AuthenticationInvalidInteraction (ie an AuthenticationSuccessfulInteraction + an error msg)
                    throw new AuthErrorInteraction(AuthErrorInteraction.Code.access_denied, "Invalid credentials");
                }
            }

            if (!Utils.isBlank(params.totp())) {
                if (TOTP.confront(params.totp(), user.totpKey())) {
                    authentications.add(TOTP_OK);
                } else {
                    //TODO [24/05/2023] AuthenticationInvalidInteraction (ie an AuthenticationSuccessfulInteraction + an error msg)
                    throw new AuthErrorInteraction(AuthErrorInteraction.Code.access_denied, "Invalid credentials");
                }
            }

            //Note : once a login form is validated, a new session WILL be created and WILL erase any previous existing sessions
            //this could happen if a client sent an authorization request with a required acr above the one linked to
            // the existing session
            var sessionId = this.sessionSupplier.createSession(user, authentications);

            var originalAuthorizeParams = this.ongoingAuthsFinder.find(params::ongoing);
            return new AuthenticationSuccessfulInteraction(sessionId, originalAuthorizeParams.orElseThrow(() -> new AuthErrorInteraction(AuthErrorInteraction.Code.server_error, "Unable to retrieve the original authorization request")));

        }



    }
}
