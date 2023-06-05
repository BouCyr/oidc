package app.cbo.oidc.java.server.endpoints.consent;

import app.cbo.oidc.java.server.backends.ongoingAuths.OngoingAuthsStorer;
import app.cbo.oidc.java.server.backends.users.UserFinder;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.endpoints.AuthErrorInteraction;
import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.Utils;

import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ConsentEndpoint {

    private final static Logger LOGGER = Logger.getLogger(ConsentEndpoint.class.getCanonicalName());


    private final OngoingAuthsStorer ongoingAuthsStorer;
    private final UserFinder userfinder;

    public ConsentEndpoint(OngoingAuthsStorer ongoingAuthsStorer, UserFinder userfinder) {
        this.ongoingAuthsStorer = ongoingAuthsStorer;
        this.userfinder = userfinder;
    }

    @NotNull
    public Interaction treatRequest(
            @NotNull Optional<Session> maybeSession,
            @NotNull ConsentParams params) throws AuthErrorInteraction {

        if (Utils.isBlank(params.clientId())) {
            throw new AuthErrorInteraction(AuthErrorInteraction.Code.unauthorized_client, "Unable to retrieve clientId");
        } else if (maybeSession.isEmpty()) {
            throw new AuthErrorInteraction(AuthErrorInteraction.Code.server_error, "Unable to handle consents without valid authentication");
        } else if (params.scopesRequested().isEmpty()) {
            throw new AuthErrorInteraction(AuthErrorInteraction.Code.server_error, "No requested scopes found");
        }

        var session = maybeSession.get();
        var user = this.userfinder.find(session.userId())
                .orElseThrow(() -> new AuthErrorInteraction(AuthErrorInteraction.Code.server_error, "Unable to retrieve user"));



        if (params.backFromForm() && params.consentGiven()) {
            //user just submitted the consent form ; add given consents
            params.scopesRequested().forEach(scope -> user.consentsTo(params.clientId(), scope));

            //TODO [01/06/2023] store the new consents
        }
        var missingConsents = params.scopesRequested()
                .stream()
                .filter(consent -> !user.hasConsentedTo(params.clientId(), consent))
                .collect(Collectors.toSet());

        if(missingConsents.isEmpty()){
            //all good !
            return new ConsentGivenInteraction(params.ongoing());
        }else{
            if(params.backFromForm()){
                //users did not give consent to all or some scopes requested by this client
                throw new AuthErrorInteraction(AuthErrorInteraction.Code.access_denied, "User did not consent");
            }else {
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
