package app.cbo.oidc.java.server.http.token;

import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import app.cbo.oidc.java.server.utils.MimeType;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class JsonError extends Exception implements Interaction {

    private final HttpCode errorCode;
    private final String msg;

    public JsonError(HttpCode errorCode, String msg) {
        this.errorCode = errorCode;
        this.msg = msg;
    }

    public JsonError(String error) {
        this(HttpCode.BAD_REQUEST, error);
    }

    public String msg() {
        return this.msg;
    }

    public HttpCode errorCode() {
        return this.errorCode;
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

        String json = "{\"error\":\"" + this.msg() + "\"}";

        exchange.getResponseHeaders().add("Content-Type", MimeType.JSON.mimeType());
        exchange.getResponseHeaders().add("Cache-Control", "no-store");
        exchange.getResponseHeaders().add("Pragma", "no-cache");
        exchange.sendResponseHeaders(errorCode().code(), json.getBytes(StandardCharsets.UTF_8).length);
        try (var os = exchange.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

    }
}
