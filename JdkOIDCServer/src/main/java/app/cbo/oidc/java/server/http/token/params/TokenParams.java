package app.cbo.oidc.java.server.http.token.params;

import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Code;
import app.cbo.oidc.java.server.jsr305.NotNull;

import java.util.Collection;
import java.util.Map;

import static app.cbo.oidc.java.server.utils.ParamsHelper.singleParam;

//3.1.3.1. Token Request
public record TokenParams(String grantType, Code code, String refreshToken, String redirectUri, ClientId clientId)
        implements CodeToTokenParams, RefreshParams {

    public final static String GRANT_TYPE = "grant_type";


    public final static String CODE = "code";

    public final static String REFRESH_TOKEN = "refresh_token";

    public final static String REDIRECT_URI = "redirect_uri";


    public final static String CLIENT_ID = "client_id";

    public TokenParams(@NotNull Map<String, Collection<String>> params) {
        this(
                singleParam(params.get(GRANT_TYPE)).orElse(null),
                singleParam(params.get(CODE)).map(Code::of).orElse(null),
                singleParam(params.get(REFRESH_TOKEN)).orElse(null),
                singleParam(params.get(REDIRECT_URI)).orElse(null),
                singleParam(params.get(CLIENT_ID)).map(ClientId::of).orElse(null));
    }
}
