package app.cbo.oidc.java.server.endpoints.consent;

import app.cbo.oidc.java.server.backends.ongoingAuths.OngoingAuthsFinder;
import app.cbo.oidc.java.server.datastored.OngoingAuthId;
import app.cbo.oidc.java.server.endpoints.AuthErrorInteraction;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeParams;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.ParamsHelper;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static app.cbo.oidc.java.server.utils.ParamsHelper.singleParam;

public record ConsentParams(Set<String> scopesRequested,
                            Set<String> consentGiven,
                            String clientId,
                            AuthorizeParams ongoing,
                            boolean backFromForm) {

    public static final String SCOPES_REQUESTED = "scopes";
    public static final String SCOPES_GIVEN = "given";
    public static final String CLIENT_ID = "client_id";
    public static final String ONGOING = "ongoing";
    public static final String BACK = "backFromForm";

    //TODO [05/05/2023]  static method
    public ConsentParams(@NotNull OngoingAuthsFinder finder, @NotNull Map<String, Collection<String>> params) throws AuthErrorInteraction {
        this(
                singleParam(params.get(SCOPES_REQUESTED))
                        .map(ParamsHelper::spaceSeparatedList)
                        .map(Set::copyOf)
                        .orElse(Set.of("openid")),
                singleParam(params.get(SCOPES_GIVEN))
                        .map(ParamsHelper::spaceSeparatedList)
                        .map(Set::copyOf)
                        .orElse(Collections.emptySet()),
                singleParam(params.get(CLIENT_ID)).orElse(null),
                finder.find(OngoingAuthId.of(singleParam(params.get(ONGOING)).orElse(null))).orElseThrow(() -> new AuthErrorInteraction(AuthErrorInteraction.Code.server_error, "unable to retrieve ongoing authentication")),
                singleParam(params.get(BACK)).map(Boolean::parseBoolean).orElse(false));

    }


}