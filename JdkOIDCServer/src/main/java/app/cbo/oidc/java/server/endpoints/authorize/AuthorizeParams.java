package app.cbo.oidc.java.server.endpoints.authorize;

import app.cbo.oidc.java.server.endpoints.AuthErrorInteraction;
import app.cbo.oidc.java.server.oidc.OIDCDisplayValues;
import app.cbo.oidc.java.server.oidc.OIDCFlow;
import app.cbo.oidc.java.server.oidc.OIDCPromptValues;
import app.cbo.oidc.java.server.utils.EnumValuesHelper;
import app.cbo.oidc.java.server.utils.QueryStringBuilder;
import app.cbo.oidc.java.server.utils.Utils;

import java.util.*;

import static app.cbo.oidc.java.server.utils.ParamsHelper.singleParam;
import static app.cbo.oidc.java.server.utils.ParamsHelper.spaceSeparatedList;

public record AuthorizeParams(
        List<String> scopes,
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


    public AuthorizeParams(Map<String, Collection<String>> params){
        this(
                spaceSeparatedList(singleParam(params.get("scope")).orElse("")),
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
                spaceSeparatedList(singleParam(params.get("ui_locales")).orElse("")),
                singleParam(params.get("id_token_hint")),
                singleParam(params.get("login_hint")),
                spaceSeparatedList(singleParam(params.get("acr_values")).orElse(""))
        );

    }

    /**
     * Checks whether the params were correctly filled by the client
     *
     * @param p the parsed params
     * @throws AuthErrorInteraction if some params were invalid
     */
    public static void checkParams(AuthorizeParams p) throws AuthErrorInteraction {
        if(p == null){
            throw new AuthErrorInteraction(AuthErrorInteraction.Code.invalid_request, "Invalid request");
        }

        if (Utils.isBlank(p.scopes())) {
            throw new AuthErrorInteraction(AuthErrorInteraction.Code.invalid_scope, "'scope' param is REQUIRED'", p);
        }
        if (p.scopes().stream().filter("openid"::equals).findAny().isEmpty()) {
            throw new AuthErrorInteraction(AuthErrorInteraction.Code.invalid_scope, "'scope' param MUST contain 'openid'", p);
        }
        if (Utils.isBlank(p.responseTypes())) {
            throw new AuthErrorInteraction(AuthErrorInteraction.Code.unsupported_response_type, "'response_type' param is REQUIRED'", p);
        }
        if(Utils.isBlank(p.clientId())){
            throw new AuthErrorInteraction(AuthErrorInteraction.Code.invalid_request , "'client_id' param is REQUIRED'",p);
        }
        if(Utils.isBlank(p.redirectUri())){

            throw new AuthErrorInteraction(AuthErrorInteraction.Code.invalid_request , "'redirect_uri' param is REQUIRED'",p);
        }
        if(p.maxAge().isPresent()) {
            try {
                Long.parseLong(p.maxAge().get());
            }catch (NumberFormatException e){
                throw new AuthErrorInteraction(AuthErrorInteraction.Code.invalid_request , "'max_age' param must be an integer value'", p);
            }
        }
    }

    /**
     * Checks whether the params were correctly filled by the client FOR THIS PARTICULAR FLOW
     * @param flow the requested flow
     * @param params the authentication request params
     * @throws AuthErrorInteraction if some params were invalid
     */
    public static void checkParamsForFlow(AuthorizeParams params, OIDCFlow flow) throws AuthErrorInteraction {
        //RQ :
        // redirect_uri MUST be https for auhtorization flow, except if the client is 'confidential'
        // redirect_uri MUST be https OR http://localhost for implicit flow.
        // CBO : I chose not to implement this ; this server is meant to be used as a dvpt crutch, TLS is not a given


        //"nonce is REQUIRED" for implicit flow
        if (flow == OIDCFlow.IMPLICIT && Utils.isBlank(params.nonce()))
            throw new AuthErrorInteraction(AuthErrorInteraction.Code.invalid_request, "'nonce' param is REQUIRED", params);

        //query response_mode is forbidden for IMPLICIT & HYBRID flow
        if (EnumSet.of(OIDCFlow.IMPLICIT, OIDCFlow.HYBRID).contains(flow)
                && params.responseMode.isPresent() && params.responseMode.get().equals("query")) {
            throw new AuthErrorInteraction(AuthErrorInteraction.Code.invalid_request, "'query' response mode is forbidden for flow '" + flow + "'", params);
        }

    }


    /**
     * Used to rebuild an authentication request from fields
     *
     * @return a query string ("?" char not included, "&"s included)
     */
    public String toQueryString() {

        //please note '.add' will not append anything to the query string if the param is null/tempty
        // and toSingle & etc. will return empty if the value is Optional.empty
        return new QueryStringBuilder()
                .add(toSpaceSeparated("scope", scopes()))
                .add(toSpaceSeparated("response_type", responseTypes()))
                .add(toSingle("client_id", clientId()))
                .add(toSingle("redirect_uri", redirectUri()))
                .add(toSingle("state", state()))
                .add(toSingle("response_mode", responseMode()))
                .add(toSingle("nonce", nonce()))
                .add(toSingle("display", display().map(OIDCDisplayValues::paramValue)))
                .add(toSpaceSeparated("prompt", prompt().stream().map(OIDCPromptValues::paramValue).toList()))
                .add(toSingle("max_age", maxAge()))
                .add(toSpaceSeparated("ui_locales", uiLocales()))
                .add(toSingle("id_token_hint", idTokenHint()))
                .add(toSingle("login_hint", loginHint()))
                .add(toSpaceSeparated("acr_values", acrValues()))
                .toString();


    }

    //TODO [03/04/2023] in .utils ?
    private String toSingle(String key, Optional<String> value) {

        return value.map(v -> key+"="+v).orElse("");
    }

    //TODO [03/04/2023] in .utils ?
    private String toSpaceSeparated(String key, List<String> values) {
        StringBuilder builder = new StringBuilder();
        if(!Utils.isEmpty(values)){
            builder
                    .append(key)
                    .append("=")
                    .append(String.join(" ", values));
        }
        return builder.toString();
    }


}
