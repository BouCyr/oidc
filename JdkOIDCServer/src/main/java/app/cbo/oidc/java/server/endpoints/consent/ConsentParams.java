package app.cbo.oidc.java.server.endpoints.consent;

import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.ParamsHelper;

import java.util.*;

import static app.cbo.oidc.java.server.utils.ParamsHelper.singleParam;

public record ConsentParams (Set<String> scopesRequested,
                             Set<String> consentGiven,
                             String clientId,
                             String ongoing,
                             boolean backFromForm) {

    public static final String SCOPES_REQUESTED = "scopes";
    public static final String SCOPES_GIVEN = "given";
    public static final String CLIENT_ID = "client_id";
    public static final String ONGOING = "ongoing";
    public static final String BACK = "backFromForm";

    public ConsentParams(@NotNull Map<String, Collection<String>> params) {
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
                singleParam(params.get(ONGOING)).orElse(null),
                singleParam(params.get(BACK)).map(Boolean::parseBoolean).orElse(false));

    }


}