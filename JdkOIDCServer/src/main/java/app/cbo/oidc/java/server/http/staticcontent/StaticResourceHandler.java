package app.cbo.oidc.java.server.http.staticcontent;

import app.cbo.oidc.java.server.http.HttpHandlerWithPath;
import app.cbo.oidc.java.server.scan.Injectable;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

@Injectable
public class StaticResourceHandler implements HttpHandlerWithPath {

    public static final String STATIC = "/sc/";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        new ResourceInteraction(exchange.getRequestURI().getPath()).handle(exchange);
    }

    @Override
    public String path() {
        return STATIC;
    }
}
