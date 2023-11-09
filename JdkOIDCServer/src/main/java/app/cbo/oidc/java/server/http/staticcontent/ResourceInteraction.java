package app.cbo.oidc.java.server.http.staticcontent;

import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.utils.HttpCode;
import app.cbo.oidc.java.server.utils.MimeType;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.function.Supplier;
import java.util.logging.Logger;

record ResourceInteraction(String path) implements Interaction {

    private static final Logger LOGGER = Logger.getLogger(ResourceInteraction.class.getCanonicalName());


    @Override
    public void handle(@NotNull HttpExchange exchange) throws IOException {

        var fileName = path.substring("/sc/".length());
        LOGGER.fine("Someone reached for static content : " + fileName);


        try (var is = Resources.getResource(fileName).map(Supplier::get).orElse(null)) {

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
