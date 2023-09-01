package app.cbo.oidc.java.server.endpoints.config;

import app.cbo.oidc.java.server.HttpHandlerWithPath;
import app.cbo.oidc.java.server.utils.HttpCode;
import app.cbo.oidc.java.server.utils.MimeType;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class ConfigHandler implements HttpHandlerWithPath {


    //TODO configure generic prefix to match with our 'standard implementation'
    public static final String CONFIG_ENDPOINT = "/.well-known/openid-configuration";
    /*

    {
    "issuer": "http://localhost:8080/auth/realms/demo",
    "authorization_endpoint": "http://localhost:8080/auth/realms/demo/protocol/openid-connect/auth",
    "token_endpoint": "http://localhost:8080/auth/realms/demo/protocol/openid-connect/token",
    "userinfo_endpoint": "http://localhost:8080/auth/realms/demo/protocol/openid-connect/userinfo",
    "end_session_endpoint": "http://localhost:8080/auth/realms/demo/protocol/openid-connect/logout",
    "jwks_uri": "http://localhost:8080/auth/realms/demo/protocol/openid-connect/certs",
    "grant_types_supported": [
        "authorization_code",
        "refresh_token",
        "password"
    ],
    "response_types_supported": [
        "code"
    ],
    "subject_types_supported": [
        "public"
    ],
    "id_token_signing_alg_values_supported": [
        "RS256"
    ],
    "response_modes_supported": [
        "query"
    ]
}

     */
    private final static Logger LOGGER = Logger.getLogger(ConfigHandler.class.getCanonicalName());
    private final String authorizationPath;
    private final String tokenPath;
    private final String userinfoPath;
    private final String logoutPath;
    private final String jwksPath;

    public ConfigHandler(String authorizationPath, String tokenPath, String userinfoPath, String logoutPath, String jwksPath) {
        this.authorizationPath = authorizationPath;
        this.tokenPath = tokenPath;
        this.userinfoPath = userinfoPath;
        this.logoutPath = logoutPath;
        this.jwksPath = jwksPath;
    }

    @Override
    public String path() {
        return CONFIG_ENDPOINT;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {


        LOGGER.info("Configuration endpoint called");
        var json = "{\n" +
                "    \"issuer\": \"http://localhost:9451\",\n" +
                "    \"authorization_endpoint\": \"" + this.authorizationPath + "\",\n" +
                "    \"token_endpoint\": \"" + this.tokenPath + "\",\n" +
                "    \"userinfo_endpoint\": \"" + this.userinfoPath + "\",\n" +
                "    \"end_session_endpoint\": \"" + this.logoutPath + "\",\n" +
                "    \"jwks_uri\": \"" + this.jwksPath + "\",\n" +
                "    \"grant_types_supported\": [\n" +
                "        \"authorization_code\",\n" +
                "        \"refresh_token\"" +
                "    ],\n" +
                "    \"response_types_supported\": [\n" +
                "        \"code\"\n" + //TODO [01/09/2023] Pretty sure we allow more :)
                "    ],\n" +
                "    \"subject_types_supported\": [\n" +
                "        \"public\"\n" +//TODO [01/09/2023] ?
                "    ],\n" +
                "    \"id_token_signing_alg_values_supported\": [\n" +
                "        \"RS256\"\n" + //TODO [01/09/2023] String.join(JWA...)
                "    ],\n" +
                "    \"response_modes_supported\": [\n" +
                "        \"query\"\n" + //TODO [01/09/2023]
                "    ]\n" +
                "}";


        exchange.getResponseHeaders().add("Content-Type", MimeType.JSON.mimeType());
        //TODO [01/09/2023] here it would make sense to allow some caching
        exchange.getResponseHeaders().add("Cache-Control", "no-store");
        exchange.getResponseHeaders().add("Pragma", "no-cache");
        exchange.sendResponseHeaders(HttpCode.OK.code(), json.getBytes(StandardCharsets.UTF_8).length);
        try (var os = exchange.getResponseBody()) {
            os.write(json.getBytes(StandardCharsets.UTF_8));
            os.flush();
        }
    }
}