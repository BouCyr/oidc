package app.cbo.oidc.java.server.endpoints.jwks;

import app.cbo.oidc.java.server.backends.KeySet;
import app.cbo.oidc.java.server.json.JSON;
import app.cbo.oidc.java.server.utils.HttpCode;
import app.cbo.oidc.java.server.utils.MimeType;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class JWKSHandler implements HttpHandler {

    public static final String JWKS_ENDPOINT = "/jwks";
    private static final Logger LOGGER = Logger.getLogger(JWKSHandler.class.getCanonicalName());

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        LOGGER.info("Someone reached for the JsonWebKeySet");
        //no endpoint here, since logic is pretty simple


        String json = JSON.jsonify(KeySet.getInstance().asJWKSet());

        exchange.getResponseHeaders().add("Content-Type", MimeType.JWKSET.mimeType());
        //[24/04/2023] here it would make sense to allow some caching
        exchange.getResponseHeaders().add("Cache-Control", "no-store");
        exchange.getResponseHeaders().add("Pragma", "no-cache");
        exchange.sendResponseHeaders(HttpCode.OK.code(), json.getBytes(StandardCharsets.UTF_8).length);
        try (var os = exchange.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }

    }
}
