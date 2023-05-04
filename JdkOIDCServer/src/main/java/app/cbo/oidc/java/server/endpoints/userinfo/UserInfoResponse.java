package app.cbo.oidc.java.server.endpoints.userinfo;

import app.cbo.oidc.java.server.endpoints.Interaction;
import app.cbo.oidc.java.server.json.JSON;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import app.cbo.oidc.java.server.utils.MimeType;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public record UserInfoResponse(Map<String, Object> userInfo) implements Interaction {

    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {
        try {
            String json = JSON.jsonify(this.userInfo());
            exchange.getResponseHeaders().add("Content-Type", MimeType.JSON.mimeType());
            exchange.getResponseHeaders().add("Cache-Control", "no-store");
            exchange.getResponseHeaders().add("Pragma", "no-cache");
            exchange.sendResponseHeaders(HttpCode.OK.code(), json.getBytes(StandardCharsets.UTF_8).length);


            try (var os = exchange.getResponseBody()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        } catch (Throwable e) {
            e.printStackTrace();
            exchange.getResponseBody().flush();
        }


    }
}
