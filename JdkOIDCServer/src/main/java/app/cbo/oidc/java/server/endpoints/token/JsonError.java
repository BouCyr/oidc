package app.cbo.oidc.java.server.endpoints.token;

import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public record JsonError(HttpCode errorCode, String msg) implements Interaction {

    public JsonError(String error) {
        this(HttpCode.BAD_REQUEST, error);
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

        exchange.getResponseHeaders().add("Cache-Control", "no-store");
        exchange.getResponseHeaders().add("Pragma", "no-cache");
        exchange.sendResponseHeaders(errorCode().code(), json.getBytes(StandardCharsets.UTF_8).length);
        try (var os = exchange.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

    }
}
