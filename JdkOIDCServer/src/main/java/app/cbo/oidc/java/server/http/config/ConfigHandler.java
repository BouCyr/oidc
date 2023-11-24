package app.cbo.oidc.java.server.http.config;

import app.cbo.oidc.java.server.http.HttpHandlerWithPath;
import app.cbo.oidc.java.server.http.authorize.AuthorizeHandler;
import app.cbo.oidc.java.server.http.jwks.JWKSHandler;
import app.cbo.oidc.java.server.http.token.TokenHandler;
import app.cbo.oidc.java.server.http.userinfo.UserInfoHandler;
import app.cbo.oidc.java.server.oidc.Issuer;
import app.cbo.oidc.java.server.scan.BuildWith;
import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.utils.HttpCode;
import app.cbo.oidc.java.server.utils.MimeType;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

@Injectable
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
    private final Issuer myself;

    @Deprecated
    public ConfigHandler(
            Issuer myself,
            String authorizationPath,
            String tokenPath,
            String userinfoPath,
            String logoutPath,
            String jwksPath) {
        this.myself = myself;
        this.authorizationPath = authorizationPath;
        this.tokenPath = tokenPath;
        this.userinfoPath = userinfoPath;
        this.logoutPath = logoutPath;
        this.jwksPath = jwksPath;
    }

    @BuildWith
    public ConfigHandler(Issuer myself,
                         AuthorizeHandler authorizeHandler,
                         TokenHandler tokenHandler,
                         UserInfoHandler userInfoHandler,
                         //TODO [24/11/2023] LogoutHandler
                         JWKSHandler jwksHandler
    ) {
        this.myself = myself;
        this.authorizationPath = myself.getIssuerId() + authorizeHandler.path();
        this.tokenPath = myself.getIssuerId() + tokenHandler.path();
        this.userinfoPath = myself.getIssuerId() + userInfoHandler.path();
        this.logoutPath = myself.getIssuerId() + "/logout"; //TODO [24/11/2023] +authorizeHandler.path();
        this.jwksPath = myself.getIssuerId() + jwksHandler.path();
    }

    @Override
    public String path() {
        return CONFIG_ENDPOINT;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {


        LOGGER.info("Configuration endpoint called");
        //TODO [01/09/2023] subject_types_supported / response_types_supported /id_token_signing_alg_values_supported
        var json = """
                    {
                    "issuer": "%s",
                    "authorization_endpoint": "%s",
                    "token_endpoint": "%s",
                    "userinfo_endpoint": "%s",
                    "end_session_endpoint": "%s",
                    "jwks_uri": "%s",
                    "grant_types_supported": [
                        "authorization_code",
                        "refresh_token"
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
                }""".formatted(
                this.myself.getIssuerId(),
                this.authorizationPath,
                this.tokenPath,
                this.userinfoPath,
                this.logoutPath,
                this.jwksPath);


        final var jsonBytes = json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", MimeType.JSON.mimeType());
        //TODO [01/09/2023] here it would make sense to allow some caching
        exchange.getResponseHeaders().add("Cache-Control", "no-store");
        exchange.getResponseHeaders().add("Pragma", "no-cache");
        exchange.sendResponseHeaders(HttpCode.OK.code(), jsonBytes.length);

        try (var os = exchange.getResponseBody()) {
            os.write(jsonBytes);
            os.flush();
        }
    }
}