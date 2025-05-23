package app.cbo.oidc.java.server.http.token;

import app.cbo.oidc.java.server.http.HttpHandlerWithPath;
import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.http.token.JsonError;
import app.cbo.oidc.java.server.http.userinfo.ForbiddenResponse;
import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.utils.HttpCode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.logging.Logger;

import static app.cbo.oidc.java.server.utils.ParamsHelper.extractParams;
import static app.cbo.oidc.java.server.utils.ParamsHelper.findClientCreds;

@Injectable
public class TokenHandler implements HttpHandlerWithPath {

    public static final String TOKEN_ENDPOINT = "/token";
    private final static Logger LOGGER = Logger.getLogger(TokenHandler.class.getCanonicalName());
    private final TokenEndpoint tokenEndpoint;

    public TokenHandler(TokenEndpoint tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    @Override
    public String path() {
        return TOKEN_ENDPOINT;
    }


    @Override
    public void handleInternal(HttpExchange exchange) throws IOException {
        try {
            final var creds = findClientCreds(exchange);
            TokenParams param = new TokenParams(extractParams(exchange));

            if (param.clientId() != null && creds.isPresent() && !param.clientId().equals(creds.get().clientId())) {
                throw new JsonError("invalid_request", "client_id in request body does not match authenticated client");
            }

            this.tokenEndpoint
                    .treatRequest(
                            param,
                            creds.isPresent() && creds.get().clientId() != null ? creds.get().clientId() : null,
                            creds.isPresent() && creds.get().clientSecret() != null ? creds.get().clientSecret() : null)
                    .handle(exchange);


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
