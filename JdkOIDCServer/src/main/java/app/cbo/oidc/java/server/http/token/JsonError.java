package app.cbo.oidc.java.server.http.token;

import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import app.cbo.oidc.java.server.utils.MimeType;
import app.cbo.oidc.java.server.utils.Utils;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * 3.1.3.4.  Token Error Response
 * The HTTP response body uses the application/json media type with HTTP response code of 400.
 *
 * <p>
 * <code>
 * HTTP/1.1 400 Bad Request
 * Content-Type: application/json
 * Cache-Control: no-store
 * Pragma: no-cache
 * <p>
 * {
 * "error": "invalid_request"
 * }
 * </code>
 * </p>
 */
public class JsonError extends Exception implements Interaction {

    private final static Logger LOGGER = Logger.getLogger(JsonError.class.getCanonicalName());

    private final String errorDescription;
    private final Cause cause;

    public JsonError(Cause msg) {
        this.cause = msg;
        this.errorDescription = "";
    }

    public JsonError(Cause msg, String errorDescription) {
        this.cause = msg;
        this.errorDescription = errorDescription;
    }

    Cause cause() {
        return cause;
    }

    public String msg() {
        return this.errorDescription;
    }

    public String json() {
        return "{" +
                "\"error\":\"" + this.cause().name() + "\"" +
                (!Utils.isBlank(msg()) ? ",\"error_description\":\"" + this.msg() + "\"" : "") +
                "}";
    }

    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {
        LOGGER.warning("Returning JsonError");
        LOGGER.warning("msg : " + msg());

        var json = this.json();
        exchange.getResponseHeaders().add("Content-Type", MimeType.JSON.mimeType());
        exchange.getResponseHeaders().add("Cache-Control", "no-store");
        exchange.getResponseHeaders().add("Pragma", "no-cache");
        exchange.sendResponseHeaders(HttpCode.BAD_REQUEST.code(), json.getBytes(StandardCharsets.UTF_8).length);
        try (var os = exchange.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

    }
/*
 HTTP/1.1 400 Bad Request
  Content-Type: application/json
  Cache-Control: no-store
  Pragma: no-cache

  {
   "error": "invalid_request"
  }
     */

    public enum Cause {
        invalid_request,
        invalid_client, //incl failed client authentication
        invalid_grant, //code or refresh token invalid (expired...)
        unauthorized_client, //  The authenticated client is not authorized to use this authorization grant type.
        unsupported_grant_type, // The authorization grant type is not supported by the authorization server.
        invalid_scope // The requested scope is invalid, unknown, malformed, or exceeds the scope granted by the resource owner.

    }
}
