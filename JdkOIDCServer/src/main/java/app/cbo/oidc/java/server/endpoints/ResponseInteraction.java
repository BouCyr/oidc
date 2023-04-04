package app.cbo.oidc.java.server.endpoints;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

public class ResponseInteraction implements Interaction {

    private final Supplier<InputStream> body;
    private final String contentType;

    public ResponseInteraction(String contentType, Supplier<InputStream> data) {
        this.body = data;
        this.contentType = contentType;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        exchange.getResponseHeaders().set("Content-type",this.contentType);
        exchange.sendResponseHeaders(200, 0);


        try(var is = body.get();
            var os = exchange.getResponseBody();){
            is.transferTo(os);
            os.flush();
        }
    }
}
