package app.cbo.oidc.toyidc.endpoints;

import app.cbo.oidc.toyidc.payloads.OAuthError;
import app.cbo.oidc.toyidc.functions.ClientFinder;
import app.cbo.oidc.toyidc.functions.CodeProvider;
import app.cbo.oidc.toyidc.payloads.TokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

@Controller
public class Authorization {

    //KC endpoints (docs keycloak 4.2.4)
    /*

    well-know :        /realms/{realm-name}/.well-known/openid-configuration

    Authorization:     /realms/{realm-name}/protocol/openid-connect/auth
    Token Endpoint:    /realms/{realm-name}/protocol/openid-connect/token
    Userinfo Endpoint: /realms/{realm-name}/protocol/openid-connect/userinfo

    jwks_uri :         /auth/realms/master/protocol/openid-connect/certs

     */

    private final Logger log = LoggerFactory.getLogger(Authorization.class);

    private final ClientFinder clientFinder;
    private final CodeProvider codeProvider;

    @Autowired
    public Authorization(ClientFinder clientFinder, CodeProvider codeProvider) {
        this.clientFinder = clientFinder;
        this.codeProvider = codeProvider;
    }

    /**
     * @see <a href="https://openid.net/specs/openid-connect-core-1_0.html#AuthorizationEndpoint#3.1.2">SPEC</a>
     *
     * The Authorization Endpoint performs Authentication of the End-User. This is done by sending the User Agent to the Authorization Server's Authorization Endpoint for Authentication and Authorization, using request parameters defined by OAuth 2.0 and additional parameters and parameter values defined by OpenID Connect.
     *
     * Communication with the Authorization Endpoint MUST utilize TLS. See Section 16.17 for more information on using TLS.
     * @return
     */
    //Authorization Servers MUST support the use of the HTTP GET and POST methods defined in RFC 2616 [RFC2616] at the Authorization Endpoint.
    // Clients MAY use the HTTP GET or POST methods to send the Authorization Request to the Authorization Server.
    // If using the HTTP GET method, the request parameters are serialized using URI Query String Serialization, per Section 13.1.
    // If using the HTTP POST method, the request parameters are serialized using Form Serialization, per Section 13.2.
    @RequestMapping(method={RequestMethod.GET, RequestMethod.POST}, path="/auth")
    public ResponseEntity<?> authentication(
            //REQUIRED. OpenID Connect requests MUST contain the openid scope value. If the openid scope value is not present, the behavior is entirely unspecified. Other scope values MAY be present.
            // Scope values used that are not understood by an implementation SHOULD be ignored.
            //TODO [17/11/2021] See Sections 5.4 and 11 for additional scope values defined by this specification.
            @RequestParam(name="scope", required = false) String scope,
            //REQUIRED. OAuth 2.0 Response Type value that determines the authorization processing flow to be used, including what parameters are returned from the endpoints used.
            // When using the Authorization Code Flow, this value is code.
            @RequestParam(name="response_type", required = false) String responseType,
            //REQUIRED. OAuth 2.0 Client Identifier valid at the Authorization Server.
            @RequestParam(name="client_id", required = false) String clientId,
            //REQUIRED. Redirection URI to which the response will be sent.
            // This URI MUST exactly match one of the Redirection URI values for the Client pre-registered at the OpenID Provider, with the matching performed as described in Section 6.2.1 of [RFC3986] (Simple String Comparison).
            // When using this flow, the Redirection URI SHOULD use the https scheme;
            // however, it MAY use the http scheme, provided that the Client Type is confidential, as defined in Section 2.1 of OAuth 2.0, and provided the OP allows the use of http Redirection URIs in this case.
            // The Redirection URI MAY use an alternate scheme, such as one that is intended to identify a callback into a native application.
            @RequestParam(name="redirect_uri", required = true) String redirectUri,
            //RECOMMENDED. Opaque value used to maintain state between the request and the callback. Typically, Cross-Site Request Forgery (CSRF, XSRF) mitigation is done by cryptographically binding the value of this parameter with a browser cookie.
            @RequestParam(name="state", required = false) String state,
            //OPTIONAL. String value used to associate a Client session with an ID Token, and to mitigate replay attacks.
            // The value is passed through unmodified from the Authentication Request to the ID Token.
            // Sufficient entropy MUST be present in the nonce values used to prevent attackers from guessing values. For implementation notes, see Section 15.5.2.
            @RequestParam(name="nonce", required = false) String nonce

/*
    display : OPTIONAL. ASCII string value that specifies how the Authorization Server displays the authentication and consent user interface pages to the End-User. The defined values are:
       page, touch, wap
    prompt : OPTIONAL. Space delimited, case sensitive list of ASCII string values that specifies whether the Authorization Server prompts the End-User for reauthentication and consent. The defined values are:
       none, login, consent, select_accout
    max_age : OPTIONAL. Maximum Authentication Age. Specifies the allowable elapsed time in seconds since the last time the End-User was actively authenticated by the OP. If the elapsed time is greater than this value, the OP MUST attempt to actively re-authenticate the End-User. (The max_age request parameter corresponds to the OpenID 2.0 PAPE [OpenID.PAPE] max_auth_age request parameter.) When max_age is used, the ID Token returned MUST include an auth_time Claim Value.
    ui_locales : OPTIONAL. End-User's preferred languages and scripts for the user interface, represented as a space-separated list of BCP47 [RFC5646] language tag values, ordered by preference. For instance, the value "fr-CA fr en" represents a preference for French as spoken in Canada, then French (without a region designation), followed by English (without a region designation). An error SHOULD NOT result if some or all of the requested locales are not supported by the OpenID Provider.
    id_token_hint : OPTIONAL. ID Token previously issued by the Authorization Server being passed as a hint about the End-User's current or past authenticated session with the Client. If the End-User identified by the ID Token is logged in or is logged in by the request, then the Authorization Server returns a positive response; otherwise, it SHOULD return an error, such as login_required. When possible, an id_token_hint SHOULD be present when prompt=none is used and an invalid_request error MAY be returned if it is not; however, the server SHOULD respond successfully when possible, even if it is not present. The Authorization Server need not be listed as an audience of the ID Token when it is used as an id_token_hint value. If the ID Token received by the RP from the OP is encrypted, to use it as an id_token_hint, the Client MUST decrypt the signed ID Token contained within the encrypted ID Token. The Client MAY re-encrypt the signed ID token to the Authentication Server using a key that enables the server to decrypt the ID Token, and use the re-encrypted ID token as the id_token_hint value.
    login_hint : OPTIONAL. Hint to the Authorization Server about the login identifier the End-User might use to log in (if necessary). This hint can be used by an RP if it first asks the End-User for their e-mail address (or other identifier) and then wants to pass that value as a hint to the discovered authorization service. It is RECOMMENDED that the hint value match the value used for discovery. This value MAY also be a phone number in the format specified for the phone_number Claim. The use of this parameter is left to the OP's discretion.
    acr_values : OPTIONAL. Requested Authentication Context Class Reference values. Space-separated string that specifies the acr values that the Authorization Server is being requested to use for processing this Authentication Request, with the values appearing in order of preference. The Authentication Context Class satisfied by the authentication performed is returned as the acr Claim Value, as specified in Section 2. The acr Claim is requested as a Voluntary Claim by this parameter. Other parameters MAY be sent. See Sections 3.2.2, 3.3.2, 5.2, 5.5, 6, and 7.2.1 for additional Authorization Request parameters and parameter values defined by this specification.
*/

    ) {

        var optClient = this.clientFinder.locate(clientId);
        if(optClient.isEmpty()){
            //we have no way to know where to send the error, so we return an error using HTTP code 400
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Client MUST provide a valid 'client_id' ");
        }
        var client = optClient.get();
        if(!client.validateRedirectUri(redirectUri)){
            //the redirectUri in request params is invalid, send the error to the ne registered in configuration
            return new OAuthError(OAuthError.Code.invalid_request,
                    "Invalid 'redirect_uri' for requested client",
                    null,
                    state).redirect(client.redirectUri);
        }

        if(scope.isEmpty()){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Client MUST provide a valid 'scope' ");
        }

        var chosenFlow = Flow.fromResponseType(responseType);
        if(chosenFlow.isEmpty()){
            return new OAuthError(
                    OAuthError.Code.unsupported_response_type,
                    responseType+" is not a valid response_type ",
                    null,
                    state).redirect(redirectUri);
        }

        switch (chosenFlow.orElse(null)){
            case AuthorizationCode:
                return this.authorizationCode(scope, clientId, redirectUri, state, nonce);
            case Implicit:
                return this.implicit(scope, clientId, redirectUri, state, nonce, false);
            case ImplicaitWithAccessToken:
                return this.implicit(scope, clientId, redirectUri, state, nonce, true);
            case HybridWithAccessToken:
                return this.hybrid(scope, clientId, redirectUri, state, nonce, true, false);
            case HybridWithId:
                return this.hybrid(scope, clientId, redirectUri, state, nonce, false, true);
            case HybridWithBoth:
                return this.hybrid(scope, clientId, redirectUri, state, nonce, true, true);
            default:
                return new OAuthError(
                    OAuthError.Code.invalid_request,
                    responseType+" is not a valid response_type ",
                    null,
                    state).redirect(redirectUri);
        }
        

    }


    private ResponseEntity<?> authorizationCode(String scope, String clientId, String redirectUri, String state, String nonce) {
        if (!"openid".equals(scope)) {

            return new OAuthError(
                    OAuthError.Code.invalid_request,
                    "scope param MUST BE 'openid' when using Authorization Code flow",
                    null,
                    state).redirect(redirectUri);
        }


        var hardCodedUserId = "user#1";
        //TODO [17/11/2021] display a page for user selection

        var authCode = this.codeProvider.store(clientId, nonce, hardCodedUserId, redirectUri);


        var builder = UriComponentsBuilder.fromHttpUrl(redirectUri);
        builder.queryParam("code", authCode);
        if(!ObjectUtils.isEmpty(state)){
            builder.queryParam("state", state);
        }
        var headers = new HttpHeaders();
        headers.setLocation(builder.build().toUri());
        return ResponseEntity.status(HttpStatus.FOUND)
                .headers(headers)
                .body(null);

    }


    private ResponseEntity<?> implicit(String scope, String clientId, String redirectUri, String state, String nonce, boolean withAccessToken) {

        //TODO [17/11/2021] scope managment "openid profile" is valid...
        if (!"openid".equals(scope)) {
            return new OAuthError(
                    OAuthError.Code.invalid_request,
                    "scope param MUST BE 'openid' when using Authorization Code flow",
                    null,
                    state).redirect(redirectUri);
        }


        var hardCodedUserId = "user#1";


        //hybrid =>  we redirect with the response in FRAGMENT (id google.fr#toto=tutu and NOT google.fr?toto=tutu)
        var builder = UriComponentsBuilder.fromHttpUrl(redirectUri);

        var access_token = "toto"; //TODO [17/11/2021] ; warning kcclient expects a jwt here ?
        var id_token = "jwtMetadata.jwtPayload.jwtSignature";
        var expires_in = Duration.ofHours(1L).toSeconds();

        StringBuilder fragment = new StringBuilder();
        fragment
                .append("token_type=").append("Bearer")
                .append("id_token=").append(id_token)
                .append("expires_in").append(expires_in);

        if(withAccessToken)
            fragment.append("access_token=")
                    .append(access_token);

        if(!ObjectUtils.isEmpty(state)){
            fragment.append("state=")
                    .append(state);
        }

        builder.fragment(fragment.toString());
        var headers = new HttpHeaders();
        headers.setLocation(builder.build().toUri());
        return ResponseEntity.status(HttpStatus.FOUND)
                .headers(headers)
                .body(null);
    }

    private ResponseEntity<?> hybrid(String scope, String clientId, String redirectUri, String state, String nonce, boolean withAccessToken, boolean withIdToken) {
        //TODO [17/11/2021] scope managment "openid profile" is valid...
        if (!"openid".equals(scope)) {
            return new OAuthError(
                    OAuthError.Code.invalid_request,
                    "scope param MUST BE 'openid' when using Authorization Code flow",
                    null,
                    state).redirect(redirectUri);
        }


        var hardCodedUserId = "user#1"; //TODO [17/11/2021]


        //hybrid =>  we redirect with the response in FRAGMENT (id google.fr#toto=tutu and NOT google.fr?toto=tutu)
        var builder = UriComponentsBuilder.fromHttpUrl(redirectUri);


        StringBuilder fragment = new StringBuilder();

        var authCode = this.codeProvider.store(clientId, nonce, redirectUri, hardCodedUserId);
        fragment.append("code=").append(authCode);

        if(withAccessToken) {
            var access_token = "toto"; //TODO [17/11/2021] ; warning kcclient expects a jwt here ?
            fragment.append("access_token=")
                    .append(access_token);
        }
        if(withIdToken){

            var id_token = "jwtMetadata.jwtPayload.jwtSignature";
            fragment.append("id_token=")
                    .append(id_token);
        }

        if(!ObjectUtils.isEmpty(state)){
            fragment.append("state=")
                    .append(state);
        }

        builder.fragment(fragment.toString());
        var headers = new HttpHeaders();
        headers.setLocation(builder.build().toUri());
        return ResponseEntity.status(HttpStatus.FOUND)
                .headers(headers)
                .body(null);
    }
}
