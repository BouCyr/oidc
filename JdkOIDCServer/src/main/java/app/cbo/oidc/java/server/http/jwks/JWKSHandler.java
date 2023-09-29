package app.cbo.oidc.java.server.http.jwks;

import app.cbo.oidc.java.server.backends.keys.KeySet;
import app.cbo.oidc.java.server.http.HttpHandlerWithPath;
import app.cbo.oidc.java.server.json.JSON;
import app.cbo.oidc.java.server.utils.HttpCode;
import app.cbo.oidc.java.server.utils.MimeType;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class JWKSHandler implements HttpHandlerWithPath {

    public static final String JWKS_ENDPOINT = "/jwks";
    private static final Logger LOGGER = Logger.getLogger(JWKSHandler.class.getCanonicalName());
    private final KeySet keyset;

    public JWKSHandler(KeySet keyset) {
        this.keyset = keyset;
    }

    @Override
    public String path() {
        return JWKS_ENDPOINT;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        LOGGER.info("Someone reached for the JsonWebKeySet");
        //no endpoint here, since logic is pretty simple


        String json = JSON.jsonify(this.keyset.asJWKSet());

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
