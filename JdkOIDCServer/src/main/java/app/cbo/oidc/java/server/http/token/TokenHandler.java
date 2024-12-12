package app.cbo.oidc.java.server.http.token;

import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.http.HttpHandlerWithPath;
import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.http.token.endpoints.CodeToTokensEndpoint;
import app.cbo.oidc.java.server.http.token.endpoints.RefreshTokenToTokensEndpoint;
import app.cbo.oidc.java.server.http.token.params.TokenParams;
import app.cbo.oidc.java.server.http.userinfo.ForbiddenResponse;
import app.cbo.oidc.java.server.scan.BuildWith;
import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.utils.HttpCode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import static app.cbo.oidc.java.server.oidc.Constants.GrantType;
import static app.cbo.oidc.java.server.utils.ParamsHelper.extractParams;
import static app.cbo.oidc.java.server.utils.Utils.isBlank;

@Injectable
public class TokenHandler implements HttpHandlerWithPath {

    public static final String TOKEN_ENDPOINT = "/token";
    private final static Logger LOGGER = Logger.getLogger(TokenHandler.class.getCanonicalName());

    private final CodeToTokensEndpoint codeToTokens;
    private final RefreshTokenToTokensEndpoint refreshTokenToTokens;

    @BuildWith
    public TokenHandler(
            CodeToTokensEndpoint codeToTokensEndpoint,
            RefreshTokenToTokensEndpoint refreshTokenToTokens) {

        this.codeToTokens = codeToTokensEndpoint;
        this.refreshTokenToTokens = refreshTokenToTokens;
    }


    @Override
    public String path() {
        return TOKEN_ENDPOINT;
    }

    @Override
    public void handleInternal(HttpExchange exchange) throws IOException {
        try {
            Map<String, Collection<String>> raw = extractParams(exchange);
            TokenParams param = new TokenParams(raw);

            var clientCreds = exchange.getRequestHeaders().get("Authorization");
            if (clientCreds == null)
                clientCreds = Collections.emptyList();


            var basicCreds = clientCreds.stream()
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith("basic "))
                    .map(s -> s.substring("basic ".length()))
                    .map(s -> new String(Base64.getDecoder().decode(s.trim())))
                    .filter(s -> s.contains(":"))
                    .findAny();

            String clientId = null;
            String clientSecret = null;
            if (basicCreds.isPresent()) {
                clientId = basicCreds.get().split(":")[0];
                clientSecret = basicCreds.get().split(":")[1];
            }


            if (!isBlank(param.refreshToken()) && !isBlank(param.code())) {
                LOGGER.warning("Both code and refresh_token provided");
                LOGGER.warning("grant_type is : " + param.grantType());
            }

            if (isBlank(param.grantType())) {
                //caught below
                throw new JsonError(JsonError.Cause.invalid_request, "no grant type");
            } else if (param.grantType().equals(GrantType.AUTHORIZATION_CODE)) {

                LOGGER.info("Grant_type is " + GrantType.AUTHORIZATION_CODE + ".");
                this.codeToTokens
                        .treatRequest(
                                param,
                                clientId != null ? ClientId.of(clientId) : null,
                                clientSecret)
                        .handle(exchange);
            } else if (param.grantType().equals(GrantType.REFRESH_TOKEN)) {
                LOGGER.info("Grant_type is " + GrantType.REFRESH_TOKEN + ".");
                this.refreshTokenToTokens.treatRequest(
                                param,
                                clientId != null ? ClientId.of(clientId) : null,
                                clientSecret)
                        .handle(exchange);
            }


        } catch (Exception e) {
            if (e instanceof Interaction i) {
                // the exception is able to handle the response
                i.handle(exchange);
            } else {
                LOGGER.severe("unexpected exception : ");
                e.printStackTrace();
                new ForbiddenResponse(HttpCode.SERVER_ERROR, ForbiddenResponse.InternalReason.TECHNICAL, "unexpected error in code/token handling").handle(exchange);
            }
        }

    }
}
