package app.cbo.oidc.java.server.endpoints.authenticate;

import java.util.Collection;
import java.util.Map;

import static app.cbo.oidc.java.server.utils.ParamsHelper.singleParam;
import static app.cbo.oidc.java.server.utils.ParamsHelper.spaceSeparatedList;

public record AuthenticateEndpointParams(String login, String password, String totp){


    public static final String LOGIN_PARAM = "login";
    public static final String PASSWORD_PARAM = "password";
    public static final String TOTP_PARAM = "totp";

    public AuthenticateEndpointParams(Map<String, Collection<String>> params){
        this(singleParam(params.get(LOGIN_PARAM)).orElse(null),
                singleParam(params.get(PASSWORD_PARAM)).orElse(null),
                singleParam(params.get(TOTP_PARAM)).orElse(null));

    }
}
