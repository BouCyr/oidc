package app.cbo.oidc.java.server.http.consent;

import app.cbo.oidc.java.server.backends.ongoingAuths.OngoingAuthsStorer;
import app.cbo.oidc.java.server.backends.users.UserFinder;
import app.cbo.oidc.java.server.backends.users.UserUpdate;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.http.AuthErrorInteraction;
import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.utils.Utils;

import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Injectable
public class ConsentEndpointImpl implements ConsentEndpoint {

    private final static Logger LOGGER = Logger.getLogger(ConsentEndpointImpl.class.getCanonicalName());


    private final OngoingAuthsStorer ongoingAuthsStorer;
    private final UserFinder userfinder;
    private final UserUpdate userUpdate;

    public ConsentEndpointImpl(OngoingAuthsStorer ongoingAuthsStorer, UserFinder userfinder, UserUpdate userUpdate) {
        this.ongoingAuthsStorer = ongoingAuthsStorer;
        this.userfinder = userfinder;
        this.userUpdate = userUpdate;
    }

    @Override
    @NotNull
    public Interaction treatRequest(
            @NotNull Optional<Session> maybeSession,
            @NotNull ConsentParams params) throws AuthErrorInteraction {

        LOGGER.fine("Handling consents for user");

        if (Utils.isBlank(params.clientId())) {
            throw new AuthErrorInteraction(AuthErrorInteraction.Code.unauthorized_client, "Unable to retrieve clientId");
        } else if (maybeSession.isEmpty()) {
            LOGGER.info("Cannot handle consents if user is not authenticated");
            throw new AuthErrorInteraction(AuthErrorInteraction.Code.session_not_found, "Unable to handle consents without valid authentication");
        } else if (params.scopesRequested().isEmpty()) {
            LOGGER.info("Cannot handle consents if user is no scopes have been requested");
            throw new AuthErrorInteraction(AuthErrorInteraction.Code.server_error, "No requested scopes found");
        }

        var session = maybeSession.get();

        var user = this.userfinder.find(session.userId())
                .orElseThrow(() -> new AuthErrorInteraction(AuthErrorInteraction.Code.user_not_found, "Unable to retrieve user"));


        //[06/10/2023] giving consent is yes/no ;  user cannot accept some and refuse others
        // yes/no has been put in 'consentGiven()' by the handler
        if (params.backFromForm() && params.consentGiven()) {
            //user just submitted the consent form ; add given consents
            params.scopesRequested().forEach(scope -> user.consentsTo(params.clientId(), scope));
            this.userUpdate.update(user);
        }
        var missingConsents = params.scopesRequested()
                .stream()
                .filter(consent -> !user.hasConsentedTo(params.clientId(), consent))
                .collect(Collectors.toSet());

        if (missingConsents.isEmpty()) {
            //all good !
            LOGGER.info("All consents were given");
            return new ConsentGivenInteraction(params.ongoing());
        } else {
            if (params.backFromForm()) {
                //users did not give consent to all or some scopes requested by this client
                throw new AuthErrorInteraction(AuthErrorInteraction.Code.access_denied, "User did not consent");
            } else {
                //some consents were never given byt his user to this client ; display the form
                return new DisplayConsentFormInteraction(
                        this.ongoingAuthsStorer.store(params.ongoing()),
                        params.ongoing(),
                        user.scopesConsentedTo(params.clientId())
                );
            }

        }
    }
}
