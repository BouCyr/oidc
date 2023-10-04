package app.cbo.oidc.java.server.http.userinfo;

import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import app.cbo.oidc.java.server.utils.MimeType;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class ForbiddenResponse extends Exception implements Interaction {


    /**
     * The request is missing a required parameter, includes an
     * unsupported parameter or parameter value, repeats the same
     * parameter, uses more than one method for including an access
     * token, or is otherwise malformed.  The resource server SHOULD
     * respond with the HTTP 400 (Bad Request) status code.
     */
    public static final String INVALID_REQUEST = "invalid_request";

    /**
     * The access token provided is expired, revoked, malformed, or
     * invalid for other reasons.  The resource SHOULD respond with
     * the HTTP 401 (Unauthorized) status code.  The client MAY
     * request a new access token and retry the protected resource
     * request.
     */
    public static final String INVALID_TOKEN = "invalid_token";

    /**
     * The request requires higher privileges than provided by the
     * access token.  The resource server SHOULD respond with the HTTP
     * 403 (Forbidden) status code and MAY include the "scope"
     * attribute with the scope necessary to access the protected
     * resource.
     */
    public static final String INSUFFICIENT_SCOPE = "insufficient_scope";

    /**
     * If the request lacks any authentication information (e.g., the client
     * was unaware that authentication is necessary or attempted using an
     * unsupported authentication method), the resource server SHOULD NOT
     * include an error code or other error information.
     */
    public static final String NO_AUTH = "";

    private final InternalReason internalReason;


    public ForbiddenResponse(HttpCode code, InternalReason internalReason, String reason) {
        super(reason);
        this.code = code;
        this.internalReason = internalReason;
    }

    private final HttpCode code;

    public InternalReason getInternalReason() {
        return internalReason;
    }

    /**
     * For logging & check reason
     * <p>(allow us to check in Unit Tests that the orbidden was created for the right reason)</p>
     */
    public enum InternalReason {
        TECHNICAL,
        MISSING_PARAMS,
        INVALID_CREDENTIALS,
        UNREADABLE_TOKEN,
        NO_TOKEN,
        WRONG_TYPE,
        EXPIRED_TOKEN,
        WRONG_ISSUER,
        INVALID_SIGNATURE,
        WRONG_CODE
    }

    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {
        //https://datatracker.ietf.org/doc/html/rfc6750

        /*
        If the protected resource request does not include authentication
   credentials or does not contain an access token that enables access
   to the protected resource, the resource server MUST include the HTTP
   "WWW-Authenticate" response header field
         */

        /*
           When a request fails, the resource server responds using the
            appropriate HTTP status code (typically, 400, 401, 403, or 405) and
            includes one of the following error codes in the response:
         */

        /*
        400 invalid request
        401 Unauthorized -> authentication required
        403 Forbidden    -> i know you are, but you have insufficient rights
        405 Method Not Allowed

         */

        exchange.getResponseHeaders().add("Content-Type", MimeType.JSON.mimeType());
        exchange.getResponseHeaders().add("Cache-Control", "no-store");
        exchange.getResponseHeaders().add("Pragma", "no-cache");

        //no body in response
        exchange.sendResponseHeaders(this.code.code(), 0);
        try (var os = exchange.getResponseBody()) {
            os.flush();
        }
    }
}
