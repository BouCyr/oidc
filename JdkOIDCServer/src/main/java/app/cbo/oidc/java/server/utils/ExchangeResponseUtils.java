package app.cbo.oidc.java.server.utils;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ExchangeResponseUtils {

    private ExchangeResponseUtils() {}


    public static void OK(HttpExchange httpExchange, String contentType, String content) throws IOException {
        build(httpExchange, HttpCode.OK, contentType, content);
    }

    public static void build(HttpExchange httpExchange, HttpCode code, String contentType, String content) throws IOException {
        if(contentType != null && !contentType.isBlank()) {
            httpExchange.getResponseHeaders().add("Content-Type", contentType);
        }

        if(content == null || content.isBlank()) {
            httpExchange.sendResponseHeaders(code.code(), 0);
        }else{
            httpExchange.sendResponseHeaders(200, content.getBytes(StandardCharsets.UTF_8).length);
            httpExchange.getResponseBody().write(content.getBytes(StandardCharsets.UTF_8));
        }

        httpExchange.getResponseBody().flush();
    }


}
