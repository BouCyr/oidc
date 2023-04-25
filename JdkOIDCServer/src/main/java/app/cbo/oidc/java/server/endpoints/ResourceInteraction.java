package app.cbo.oidc.java.server.endpoints;

import app.cbo.oidc.java.server.endpoints.authenticate.AuthenticateEndpoint;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import app.cbo.oidc.java.server.utils.MimeType;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public record ResourceInteraction(String path) implements Interaction {


    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {

        var fileName = path.substring("/sc/".length());

        try (var is = AuthenticateEndpoint.getInstance().getClass().getClassLoader().getResourceAsStream(fileName)) {

            boolean found = (is != null);
            if (!found) {
                exchange.sendResponseHeaders(HttpCode.NOT_FOUND.code(), 0);
                exchange.getResponseBody().close();
                return;
            }


            var hasExtension = fileName.lastIndexOf(".") + 1;
            if (hasExtension != 0) {
                var extension = fileName.substring(hasExtension);
                var contentType = MimeType.fromExtension(extension);


                    contentType.ifPresent(mimeType -> exchange.getResponseHeaders().add("Content-type", mimeType.mimeType()));
            }
            exchange.sendResponseHeaders(200, 0);


            try (var os = exchange.getResponseBody()) {
                is.transferTo(os);
                os.flush();
                return;
            }
        }
    }
}
