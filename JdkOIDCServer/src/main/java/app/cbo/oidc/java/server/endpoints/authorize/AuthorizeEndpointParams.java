package app.cbo.oidc.java.server.endpoints.authorize;

import app.cbo.oidc.java.server.endpoints.AuthError;
import app.cbo.oidc.java.server.oidc.OIDCDisplayValues;
import app.cbo.oidc.java.server.oidc.OIDCFlow;
import app.cbo.oidc.java.server.oidc.OIDCPromptValues;
import app.cbo.oidc.java.server.utils.EnumValuesHelper;
import app.cbo.oidc.java.server.utils.Utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static app.cbo.oidc.java.server.utils.ParamsHelper.singleParam;
import static app.cbo.oidc.java.server.utils.ParamsHelper.spaceSeparatedList;

public record AuthorizeEndpointParams(
        List<String> scope,
        List<String> responseTypes,
        Optional<String> clientId,
        Optional<String> redirectUri,
        Optional<String> state,
        Optional<String> responseMode,
        Optional<String> nonce,
        Optional<OIDCDisplayValues> display,
        List<OIDCPromptValues> prompt,
        Optional<String> maxAge,
        List<String> uiLocales,
        Optional<String> idTokenHint,
        Optional<String> loginHint,
        List<String> acrValues

        //TODO [17/03/2023] cf 5.5 'claims'
        //TODO [17/03/2023] cf 6  "Passing Request Parameters as JWTs" - this will be another nightmare
){


    public AuthorizeEndpointParams(Map<String, Collection<String>> params){
        this(spaceSeparatedList(singleParam(params.get("scope")).orElse("")),
                spaceSeparatedList(singleParam(params.get("response_type")).orElse("")),
                singleParam(params.get("client_id")),
                singleParam(params.get("redirect_uri")),
                singleParam(params.get("state")),
                singleParam(params.get("response_mode")),
                singleParam(params.get("nonce")),
                EnumValuesHelper.fromParam(//TODO [17/03/2023] handle invalid values
                        singleParam(params.get("display")).orElse(""), //map the string found in "display"
                        OIDCDisplayValues.values()),//to OIDCDisplayValues
                EnumValuesHelper.fromParams(//TODO [17/03/2023] handle invalid values
                        spaceSeparatedList(singleParam(params.get("prompt")).orElse("")),//split the string to params
                        OIDCPromptValues.values()),
                singleParam(params.get("max_age")),
                spaceSeparatedList(singleParam(params.get("ui-locales")).orElse("")),
                singleParam(params.get("id_token_hint")),
                singleParam(params.get("login_hint")),
                spaceSeparatedList(singleParam(params.get("acr-values")).orElse(""))
        );

    }

    /**
     * Checks wether the params were correctly filled by the client
     * @param p the parsed params
     * @throws AuthError if some params were invalid
     */
    public static void checkParams(AuthorizeEndpointParams p) throws AuthError {
        if(p == null){
            throw new AuthError(AuthError.Code.invalid_request, "Invalid request");
        }

        if(Utils.isBlank(p.scope())){
            throw new AuthError(AuthError.Code.invalid_scope, "'scope' param is REQUIRED'",p);
        }
        if(p.scope().stream().filter("openid"::equals).findAny().isEmpty()){
            throw new  AuthError(AuthError.Code.invalid_scope,  "'scope' param MUST contain 'openid'",p);
        }
        if(Utils.isBlank(p.responseTypes())){
            throw new  AuthError(AuthError.Code.unsupported_response_type , "'response_type' param is REQUIRED'", p);
        }
        if(Utils.isBlank(p.clientId())){
            throw new AuthError(AuthError.Code.invalid_request , "'client_id' param is REQUIRED'",p);
        }
        if(Utils.isBlank(p.redirectUri())){
            throw new AuthError(AuthError.Code.invalid_request , "'redirect_uri' param is REQUIRED'",p);
        }
        if(p.maxAge().isPresent()) {
            try {
                Long.parseLong(p.maxAge().get());
            }catch (NumberFormatException e){
                throw new AuthError(AuthError.Code.invalid_request , "'max_age' param must be an integer value'", p);
            }
        }
    }

    /**
     * Checks wether the params were correctly filled by the client FOR THIS PARTICULAR FLOW
     * @param flow the requested flow
     * @param params the authentication request params
     * @throws AuthError if some params were invalid
     */
    public static void checkParamsForFlow(AuthorizeEndpointParams params, OIDCFlow flow) throws AuthError {
        //AFAIK, only specific requirement is "nonce is REQUIRED" for implicit flow
        if(flow == OIDCFlow.IMPLICIT && Utils.isBlank(params.nonce()))
            throw new AuthError(AuthError.Code.invalid_request, "'nonce' param is REQUIRED'", params);
    }





}
