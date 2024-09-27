package app.cbo.oidc.java.server.http.introspect;

import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.http.HttpHandlerWithPath;
import app.cbo.oidc.java.server.http.token.JsonError;
import app.cbo.oidc.java.server.http.userinfo.ForbiddenResponse;
import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.utils.MimeType;
import app.cbo.oidc.java.server.utils.QueryStringParser;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Injectable
public class IntrospectionHandler implements HttpHandlerWithPath {
    public static final String INTROSPECTION_ENDPOINT = "/introspect";
    private static final Logger LOGGER = Logger.getLogger(IntrospectionHandler.class.getCanonicalName());


    private final IntrospectionEndpoint endpoint;

    public IntrospectionHandler(IntrospectionEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    private static Map<String, Collection<String>> readPostBody(HttpExchange exchange) throws JsonError {
        Map<String, Collection<String>> params;
        if (exchange.getRequestHeaders().containsKey("Content-Type")
                && exchange.getRequestHeaders().get("Content-Type").size() == 1
                && exchange.getRequestHeaders().get("Content-Type").getFirst().startsWith(MimeType.FORM.mimeType())) {

            try (var reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
                String result = reader.lines().collect(Collectors.joining("\n"));
                params = QueryStringParser.from(result);
            } catch (IOException e) {
                throw new JsonError("invalid_request");
            }

        } else {
            String msg = "POST request with wrong contentType";
            throw new JsonError("invalid_request");
        }
        return params;
    }

    @Override
    public String path() {
        return INTROSPECTION_ENDPOINT;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        LOGGER.info("Someone called the introspection endpoint");
        var clientCreds = exchange.getRequestHeaders().get("Authorization");
        if (clientCreds == null)
            clientCreds = Collections.emptyList();


        String token;
        try {
            var postBody = readPostBody(exchange);
            token = postBody.get("token").stream().findFirst().orElseThrow();
        } catch (JsonError e) {

            e.handle(exchange);
            return;
        }

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

        LOGGER.info(clientId + " called the introspection endpoint");

        try {
            var result = this.endpoint.treatRequest(
                    token,
                    ClientId.of(clientId),
                    clientSecret
            );
            LOGGER.info("Introspection done");
            result.handle(exchange);
        } catch (JsonError | ForbiddenResponse interaction) {
            LOGGER.info("Introspection done");
            interaction.handle(exchange);
        }

    }
}
