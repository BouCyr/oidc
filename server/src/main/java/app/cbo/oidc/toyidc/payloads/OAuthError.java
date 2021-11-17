package app.cbo.oidc.toyidc.payloads;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * see <a href="https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.2.1> Oauth error codes </a>
 */
public class OAuthError extends Response{

    /**
     * A single ASCII [USASCII] error code
     */
    private final OAuthError.Code error;
    /**
     * OPTIONAL.  Human-readable ASCII [USASCII] text providing additional information, used to assist the client developer in understanding the error that occurred.
     */
    private String error_description;
    /**
     * OPTIONAL.  A URI identifying a human-readable web page with information about the error, used to provide the client developer with additional information about the error.
     */
    private String error_uri;
    /**
     *  REQUIRED if a "state" parameter was present in the client authorization request.  The exact value received from the client.
     */
    private String state;

    OAuthError() {
        this.error = Code.server_error;
    }

    public OAuthError(Code error) {
        this.error = error;
    }

    public OAuthError(Code error, String state) {
        this.error = error;
        this.state = state;
    }

    public OAuthError(Code error, String error_description, String error_uri, String state) {
        this.error = error;
        this.error_description = error_description;
        this.error_uri = error_uri;
        this.state = state;
    }

    public ResponseEntity<?> redirect(String rpRedirectURL){
        var builder = UriComponentsBuilder.fromHttpUrl(rpRedirectURL);
        builder.queryParam("code", this.error.toString());
        if(!ObjectUtils.isEmpty(state)){
            builder.queryParam("state", this.error.toString());
        }
        if(!ObjectUtils.isEmpty(error_description)){
            builder.queryParam("error_description", this.error_description.toString());
        }
        if(!ObjectUtils.isEmpty(error_uri)){
            builder.queryParam("error_uri", this.error_uri.toString());
        }

        var headers = new HttpHeaders();
        headers.setLocation(builder.build().toUri());
        return ResponseEntity.status(HttpStatus.FOUND)
                .headers(headers)
                .body(null);


    }

    public enum Code {
        //OAuth2 codes :
        /**
         The request is missing a required parameter, includes an invalid parameter value, includes a parameter more than once, or is otherwise malformed.
         */
        invalid_request,
        /**
         * The client is not authorized to request an authorization code using this method.
         */
        unauthorized_client,
        /**
         * The resource owner or authorization server denied the request.
         */
        access_denied,
        /**
         * The authorization server does not support obtaining an authorization code using this method.
         */
        unsupported_response_type,
        /**
         * The requested scope is invalid, unknown, or malformed.
         */
        invalid_scope,
        /**
         * eq to HTTP status 500 (for redirects response)
         */
        server_error,
        /**
         * eq to HTTP status 503 (for redirects response)
         */
        temporarily_unavailable,
        /**
         * The provided authorization grant (e.g., authorization code, resource owner credentials) or refresh token is invalid, expired, revoked, does not match the redirection URI used in the authorization request, or was issued to another client.
         */
        invalid_grant



        /* 3.1.2.6 : specific OIDC codes :
         * interaction_required
         * The Authorization Server requires End-User interaction of some form to proceed. This error MAY be returned when the prompt parameter value in the Authentication Request is none, but the Authentication Request cannot be completed without displaying a user interface for End-User interaction.
         * login_required
         * The Authorization Server requires End-User authentication. This error MAY be returned when the prompt parameter value in the Authentication Request is none, but the Authentication Request cannot be completed without displaying a user interface for End-User authentication.
         * account_selection_required
         * The End-User is REQUIRED to select a session at the Authorization Server. The End-User MAY be authenticated at the Authorization Server with different associated accounts, but the End-User did not select a session. This error MAY be returned when the prompt parameter value in the Authentication Request is none, but the Authentication Request cannot be completed without displaying a user interface to prompt for a session to use.
         * consent_required
         * The Authorization Server requires End-User consent. This error MAY be returned when the prompt parameter value in the Authentication Request is none, but the Authentication Request cannot be completed without displaying a user interface for End-User consent.
         * invalid_request_uri
         * The request_uri in the Authorization Request returns an error or contains invalid data.
         * invalid_request_object
         * The request parameter contains an invalid Request Object.
         * request_not_supported
         * The OP does not support use of the request parameter defined in Section 6.
         * request_uri_not_supported
         * The OP does not support use of the request_uri parameter defined in Section 6.
         * registration_not_supported
         * The OP does not support use of the registration parameter defined in Section 7.2.1.
         */
    }
}
