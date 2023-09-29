package app.cbo.oidc.java.server.http.authenticate;

import app.cbo.oidc.java.server.jsr305.NotNull;

import java.util.Collection;
import java.util.Map;

import static app.cbo.oidc.java.server.utils.ParamsHelper.singleParam;

public record AuthenticateParams(String login, String password, String totp, String ongoing){


    public static final String LOGIN_PARAM = "login";
    public static final String PASSWORD_PARAM = "pwd";
    public static final String TOTP_PARAM = "totp";
    public static final String ONGOING = "ongoing";

    public AuthenticateParams(@NotNull Map<String, Collection<String>> params){
        this(singleParam(params.get(LOGIN_PARAM)).orElse(null),
                singleParam(params.get(PASSWORD_PARAM)).orElse(null),
                singleParam(params.get(TOTP_PARAM)).orElse(null),
                singleParam(params.get(ONGOING)).orElse(null));

    }
}
