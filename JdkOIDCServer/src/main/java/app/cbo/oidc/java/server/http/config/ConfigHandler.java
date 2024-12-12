package app.cbo.oidc.java.server.http.config;

import app.cbo.oidc.java.server.http.AuthErrorInteraction;
import app.cbo.oidc.java.server.http.HttpHandlerWithPath;
import app.cbo.oidc.java.server.http.PathCustomizer;
import app.cbo.oidc.java.server.http.authorize.AuthorizeHandler;
import app.cbo.oidc.java.server.http.jwks.JWKSHandler;
import app.cbo.oidc.java.server.http.token.TokenHandler;
import app.cbo.oidc.java.server.http.userinfo.UserInfoHandler;
import app.cbo.oidc.java.server.oidc.Issuer;
import app.cbo.oidc.java.server.scan.BuildWith;
import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.utils.HttpCode;
import app.cbo.oidc.java.server.utils.MimeType;
import app.cbo.oidc.java.server.utils.ParamsHelper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

import static app.cbo.oidc.java.server.oidc.Constants.GrantType;

@Injectable
public class ConfigHandler implements HttpHandlerWithPath {


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
    private final PathCustomizer pathCustomizer;


    @BuildWith
    public ConfigHandler(
            PathCustomizer pathCustomizer,
            Issuer myself,
            AuthorizeHandler authorizeHandler,
            TokenHandler tokenHandler,
            UserInfoHandler userInfoHandler,
            //TODO [24/11/2023] LogoutHandler
            JWKSHandler jwksHandler
    ) {
        this.pathCustomizer = pathCustomizer;
        this.myself = myself;
        this.authorizationPath = authorizeHandler.path();
        this.tokenPath = tokenHandler.path();
        this.userinfoPath = userInfoHandler.path();
        this.logoutPath = "/logout"; //TODO [24/11/2023] +authorizeHandler.path();
        this.jwksPath = jwksHandler.path();
    }

    @Override
    public String path() {
        return pathCustomizer.customize(CONFIG_ENDPOINT);
    }

    @Override
    public void handleInternal(HttpExchange exchange) throws IOException {


        Map<String, Collection<String>> params;
        try {
            params = ParamsHelper.extractParams(exchange);
        } catch (AuthErrorInteraction e) {
            LOGGER.warning("Invalid call to config endpoint ");
            e.handle(exchange);
            return;
        }

        //allow host override for configuration
        //probably a security issue in 'real life', but this is not meant to be used in production
        //e.g. useful when some code from inside a container must reach the server, when 'localhost' can be amibguous
        var host = ParamsHelper.singleParam(params.get("hostoverride")).orElse(this.myself.getIssuerId());



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
                        "%s",
                        "%s"
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
                host,
                url(host, this.authorizationPath),
                url(host, this.tokenPath),
                url(host, this.userinfoPath),
                url(host, this.logoutPath),
                url(host, this.jwksPath),
                GrantType.AUTHORIZATION_CODE,
                GrantType.REFRESH_TOKEN);


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

    String url(String host, String path) {
        return host + path;
    }
}