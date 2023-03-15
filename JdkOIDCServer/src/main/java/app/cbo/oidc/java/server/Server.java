package app.cbo.oidc.java.server;

import app.cbo.oidc.java.server.utils.QueryStringParser;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class Server {

    public static final String HOST_NAME = "localhost";

    private final int port;
    private final HttpServer server;

    private final HttpHandler handle = (httpExchange) -> {

        String decodedQuery = httpExchange.getRequestURI().getQuery();
        System.out.println(decodedQuery);

        var params = QueryStringParser.from(decodedQuery);
        var sb = new StringBuilder();
        params.forEach((k, v) -> {
            sb.append(k).append(":");
            if (v.isEmpty() || (v.size() == 1 && v.iterator().next().isEmpty())) {
                sb.append("[empty]").append(System.lineSeparator());
            } else if (v.size() == 1) {
                sb.append(v.iterator().next().get())
                        .append(System.lineSeparator());
            } else {
                sb.append(System.lineSeparator());
                v.stream().forEach(entry -> {
                    if (entry.isEmpty()) {
                        sb.append("  -[empty]").append(System.lineSeparator());
                    } else {
                        sb.append("  -").append(entry.get()).append(System.lineSeparator());
                    }
                });
            }
        });
        String body = sb.toString();
        if (body.isBlank()) {
            body = "[empty]";
        }
        httpExchange.getResponseHeaders().add("Content-Type", "text/plain");
        httpExchange.sendResponseHeaders(200, body.getBytes(StandardCharsets.UTF_8).length);
        httpExchange.getResponseBody().write(body.getBytes(StandardCharsets.UTF_8));
        httpExchange.getResponseBody().flush();


    };

    public Server(StartupArgs from) throws IOException {
        this.port = from.port();
        this.server = HttpServer.create(new InetSocketAddress(HOST_NAME, port), 50);

    }

    public void start() throws IOException {

        server.createContext("/test", this.handle);

        // start the server
        server.start();
        System.out.printf("Server started on host %s and port %s %n", HOST_NAME, port);
    }

    public void stop() {
        //TODO [15/03/2023] add message, maybe delay.
        this.server.stop(0);
    }

}
