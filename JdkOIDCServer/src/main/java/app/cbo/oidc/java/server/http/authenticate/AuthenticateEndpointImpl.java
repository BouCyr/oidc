package app.cbo.oidc.java.server.http.authenticate;

import app.cbo.oidc.java.server.backends.ongoingAuths.OngoingAuthsFinder;
import app.cbo.oidc.java.server.backends.sessions.SessionSupplier;
import app.cbo.oidc.java.server.backends.users.UserCreator;
import app.cbo.oidc.java.server.backends.users.UserFinder;
import app.cbo.oidc.java.server.credentials.TOTP;
import app.cbo.oidc.java.server.credentials.pwds.PasswordChecker;
import app.cbo.oidc.java.server.datastored.OngoingAuthId;
import app.cbo.oidc.java.server.datastored.user.User;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.http.AuthErrorInteraction;
import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.utils.Utils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.logging.Logger;

import static app.cbo.oidc.java.server.credentials.AuthenticationMode.*;

@Injectable
public class AuthenticateEndpointImpl implements AuthenticateEndpoint {


    private final static Logger LOGGER = Logger.getLogger(AuthenticateEndpointImpl.class.getCanonicalName());
    private final OngoingAuthsFinder ongoingAuthsFinder;
    private final UserFinder userFinder;
    private final SessionSupplier sessionSupplier;
    private final PasswordChecker passwordChecker;
    private final UserCreator userCreator;


    public AuthenticateEndpointImpl(
            OngoingAuthsFinder ongoingAuthsFinder,
            UserFinder userFinder,
            UserCreator userCreator,
            SessionSupplier sessionSupplier,
            PasswordChecker passwordChecker) {
        this.ongoingAuthsFinder = ongoingAuthsFinder;
        this.userFinder = userFinder;
        this.userCreator = userCreator;
        this.sessionSupplier = sessionSupplier;
        this.passwordChecker = passwordChecker;
    }

    @Override
    @NotNull
    public Interaction treatRequest(
            @NotNull Map<String, Collection<String>> rawParams) throws AuthErrorInteraction {

        AuthenticateParams params = new AuthenticateParams(rawParams);


        if (Utils.isBlank(params.login())) {
            LOGGER.info("No login found in params, displaying the login form");
            return new DisplayLoginFormInteraction(params.ongoing());
        } else {

            LOGGER.info("We have a login");
            var authentications = EnumSet.of(DECLARATIVE);

            var userFound = this.userFinder.find(UserId.of(params.login()));

            User user;
            if (userFound.isPresent()) {
                user = userFound.get();
                authentications.add(USER_FOUND);
                LOGGER.info("We have a matching user in storage");
            } else {
                LOGGER.info("We do not have a matching user in storage, creating one");
                var newId = this.userCreator.create(params.login(), null, null);
                user = this.userFinder.find(newId).orElseThrow(() -> new RuntimeException("Unable to retrieve the user I just created... :("));

            }


            if (!Utils.isBlank(params.password())) {
                LOGGER.info("User gave us a password");
                if (passwordChecker.confront(params.password(), user.pwd())) {
                    LOGGER.info("Given password matches the stored password");
                    authentications.add(PASSWORD_OK);
                } else {
                    //TODO [24/05/2023] AuthenticationInvalidInteraction (ie an AuthenticationSuccessfulInteraction + an error msg)
                    throw new AuthErrorInteraction(AuthErrorInteraction.Code.access_denied, "Invalid credentials");
                }
            }

            if (!Utils.isBlank(params.totp())) {
                LOGGER.info("User gave us a TOTP");
                if (TOTP.confront(params.totp(), user.totpKey())) {
                    LOGGER.info("TOTP is valid");
                    authentications.add(TOTP_OK);
                } else {
                    //TODO [24/05/2023] AuthenticationInvalidInteraction (ie an AuthenticationSuccessfulInteraction + an error msg)
                    throw new AuthErrorInteraction(AuthErrorInteraction.Code.access_denied, "Invalid credentials");
                }
            }

            var originalAuthorizeParams = this.ongoingAuthsFinder.find(new OngoingAuthId(params.ongoing()))
                    .orElseThrow(() -> new AuthErrorInteraction(AuthErrorInteraction.Code.server_error, "Unable to retrieve the original authorization request"));
            var sessionId = this.sessionSupplier.createSession(user,
                    authentications,
                    originalAuthorizeParams.scopes());

            return new AuthenticationSuccessfulInteraction(sessionId, originalAuthorizeParams);

        }


    }
}
