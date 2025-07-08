package app.cbo.oidc.java.server.http.filter;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

public class CorsFilter extends Filter {

    @Override
    public String description() {
        return "Adds CORS headers (allow all everytime everywhere :)) to response";
    }

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {

        // Add CORS headers
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        // Handle pre-flight (OPTIONS) requests immediately
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1); // 204 No Content
            return;
        }

        chain.doFilter(exchange);
    }
}
