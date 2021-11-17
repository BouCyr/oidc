package app.cbo.oidc.toyidc.endpoints;

import app.cbo.oidc.toyidc.backend.StoredCode;
import app.cbo.oidc.toyidc.functions.CodeRetriever;
import app.cbo.oidc.toyidc.payloads.OAuthError;
import app.cbo.oidc.toyidc.payloads.Response;
import app.cbo.oidc.toyidc.payloads.TokenResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.temporal.TemporalUnit;

@RestController
public class Token {

    private CodeRetriever codeRetriever;

    @PostMapping(
            path="/auth/realms/ME/protocol/openid-connect/token",
            consumes="application/x-www-form-urlencoded",
            produces = "application/json")
    public ResponseEntity<Response> token(@RequestHeader(name = "Authorization", required = false) String authorizationHeader,
                          @RequestParam(name="client_id", required=false) String client_id,
                          @RequestParam(name="client_secret", required=false) String client_secret,

                          @RequestParam(name="grant_type", required=false) String grant_type,
                          @RequestParam(name="code", required=true) String code,
                          @RequestParam(name="redirect_uri", required=false) String redirect_uri){


        // A Client makes a Token Request by presenting its Authorization Grant (in the form of an Authorization Code) to the Token Endpoint using the grant_type value authorization_code
        //[17/11/2021] this code is Ok only if this endpoint is ONLY used for authorization_codes consuming
        if(ObjectUtils.isEmpty(grant_type) || !"authorization_code".equals(grant_type)){
            return error(new OAuthError(OAuthError.Code.invalid_grant), HttpStatus.BAD_REQUEST);
        }

        // Authenticate the Client if it was issued Client Credentials or if it uses another Client Authentication method, per Section 9.
        if(!this.authenticateClient(authorizationHeader, client_id, client_secret)){
            return error(new OAuthError(OAuthError.Code.unauthorized_client), HttpStatus.BAD_REQUEST);
        }
        var optStoredCode = this.codeRetriever.retrieve(code);
        if(optStoredCode.isEmpty()) {
            return error(new OAuthError(OAuthError.Code.invalid_grant), HttpStatus.BAD_REQUEST);
        }
        var retrievedCode = optStoredCode.get();
        //Ensure the Authorization Code was issued to the authenticated Client.
        if(!retrievedCode.generatedFor.equals(client_id)){
            return error(new OAuthError(OAuthError.Code.invalid_grant), HttpStatus.BAD_REQUEST);
        }
        //Verify that the Authorization Code is valid.
        //        If possible, verify that the Authorization Code has not been previously used.
        //>> [17/11/2021] : optStoredCode would have been empty
        //        Ensure that the redirect_uri parameter value is identical to the redirect_uri parameter value that was included in the initial Authorization Request.
        //        If the redirect_uri parameter value is not present when there is only one registered redirect_uri value,
        //        the Authorization Server MAY return an error (since the Client should have included the parameter) or MAY proceed without an error (since OAuth 2.0 permits the parameter to be omitted in this case).
        if(!ObjectUtils.isEmpty(redirect_uri) && !retrievedCode.redirectUri.equals(redirect_uri)) {
            return error(new OAuthError(OAuthError.Code.invalid_grant), HttpStatus.BAD_REQUEST);
        }
        //Verify that the Authorization Code used was issued in response to an OpenID Connect Authentication Request (so that an ID Token will be returned from the Token Endpoint).
        //>> [17/11/2021] We only implement OIDC authentication, so nothing to do here ?

        var response = new TokenResponse();

        //The OAuth 2.0 token_type response parameter value MUST be Bearer, as specified in OAuth 2.0 Bearer Token Usage [RFC6750], unless another Token Type has been negotiated with the Client. Servers SHOULD support the Bearer Token Type; use of other Token Types is outside the scope of this specification.
        response.token_type = "Bearer";
        response.access_token = "toto"; //TODO [17/11/2021] ; warning kcclient expects a jwt here
        response.refresh_token = "tutu"; //TODO [17/11/2021] ; warning kcclient expects a jwt here
        response.id_token = this.tokenId(retrievedCode);
        response.expires_in = Duration.ofHours(1L).toSeconds();


        return ok(response);
    }

    private String tokenId(StoredCode retrievedCode) {
        //TODO [17/11/2021] put in a function... NONCE MUST BE IN CLAIMS
        return "jwtMetadata.jwtPayload.jwtSignature";
    }

    private ResponseEntity<Response> error(OAuthError error, HttpStatus status){
        var headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache());
        headers.setPragma("no-cache");
        return ResponseEntity.status(status).headers(headers).body(error);
    }
    private ResponseEntity<Response> ok(TokenResponse response){
        var headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noCache());
        headers.setPragma("no-cache");
        return ResponseEntity.status(HttpStatus.OK).headers(headers).body(response);
    }

    private boolean authenticateClient(String authorizationHeader, String client_id, String client_secret) {
        //TODO [17/11/2021]
        return true;
    }
}
