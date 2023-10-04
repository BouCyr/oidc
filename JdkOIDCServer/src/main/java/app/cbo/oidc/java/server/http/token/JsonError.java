package app.cbo.oidc.java.server.http.token;

import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import app.cbo.oidc.java.server.utils.MimeType;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 3.1.3.4.  Token Error Response
 * The HTTP response body uses the application/json media type with HTTP response code of 400.
 *
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
 */
public class JsonError extends Exception implements Interaction {

    private final String msg;
    private final String json;

    public JsonError(String msg) {
        this.msg = msg;
        this.json = "{\"error\":\"" + this.msg() + "\"}";
    }


    public String msg() {
        return this.msg;
    }

    public String json() {
        return json;
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

    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {


        exchange.getResponseHeaders().add("Content-Type", MimeType.JSON.mimeType());
        exchange.getResponseHeaders().add("Cache-Control", "no-store");
        exchange.getResponseHeaders().add("Pragma", "no-cache");
        exchange.sendResponseHeaders(HttpCode.BAD_REQUEST.code(), this.json.getBytes(StandardCharsets.UTF_8).length);
        try (var os = exchange.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

    }
}
