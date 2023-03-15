package app.cbo.oidc.java.server.endpoints;

import app.cbo.oidc.java.server.utils.QueryStringParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class AuthorizeEndpoint implements HttpHandler {


    @Override
    public void handle(HttpExchange exchange) throws IOException {

        Map<String, Collection<Optional<String>>> params;
        if ("GET".equals(exchange.getRequestMethod())) {
            params = QueryStringParser.from(exchange.getRequestURI().getQuery());
        } else if ("POST".equals(exchange.getRequestMethod())) {
            /*
            eg. 13.2.  Form Serialization
  POST /authorize HTTP/1.1
  Host: server.example.com
  Content-Type: application/x-www-form-urlencoded

  response_type=code
    &scope=openid
    &client_id=s6BhdRkqt3
    &redirect_uri=https%3A%2F%2Fclient.example.org%2Fcb

             */
            if (exchange.getRequestHeaders().containsKey("Content-Type")
                    && exchange.getRequestHeaders().get("Content-Type").size() == 1
                    && "application/x-www-form-urlencoded".equals(exchange.getRequestHeaders().get("Content-Type").get(0))) {

                // TODO [15/03/2023] autocloseable
                String result = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                        .lines().collect(Collectors.joining("\n"));
                params = QueryStringParser.from(result);

            }
        }
    }
}
