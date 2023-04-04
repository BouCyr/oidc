package app.cbo.oidc.java.server.utils;

import app.cbo.oidc.java.server.endpoints.AuthError;
import com.sun.net.httpserver.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collection;
import java.util.Map;

class ParamsHelperTest {


    public static final int PORT = 4502;
    static HttpServer server;

    @BeforeAll
    static void startupServer() throws IOException {
        System.out.println("--Starting http server--");
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.start();

    }

    @AfterAll
    static void stopServer(){
        if(server != null) {
            System.out.println("--Stopping http server--");
            server.stop(0);
        }
    }

    static URI uri(String path) throws URISyntaxException {
        if(!path.startsWith("/"))
            path = "/"+path;

        return new URI( localhost() + path);
    }

    private static String localhost() {
        return "http://localhost:" + PORT;
    }


    @Test
    public void test() throws Exception {


        server.createContext("/test", (hx) ->{


            Map<String, Collection<String>> params;
            try {
                params = ParamsHelper.extractParams(hx);
            } catch (AuthError authError) {
                Assertions.fail("Should work");
                throw new RuntimeException(authError);
            }
            Assertions.assertThat(params)
                    .hasSize(1)
                    .containsKey("singleParam");
            Assertions.assertThat(params.get("singleParam"))
                    .hasSize(1)
                    .element(0).isEqualTo("single");




            System.out.println("Receiving request");
            hx.sendResponseHeaders(200,0);
            hx.close();
            System.out.println("Sending response");


        });

        var c = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri("/test?singleParam=single"))
                .GET().build();

        System.out.println("Sending request");
        var h = c.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("Checking response");
        Assertions.assertThat(h.statusCode()).isEqualTo(200);

    }

    public static HttpExchange getPayload(String method, String uri) {
        return new HttpExchange() {

            @Override
            public Headers getRequestHeaders() {
                return null;
            }

            @Override
            public Headers getResponseHeaders() {
                return null;
            }

            @Override
            public URI getRequestURI() {
                try {
                    return new URI(uri);
                } catch (URISyntaxException e) {
                    return null;
                }
            }

            @Override
            public String getRequestMethod() {
                return method;
            }

            @Override
            public HttpContext getHttpContext() {
                return null;
            }

            @Override
            public void close() {

            }

            @Override
            public InputStream getRequestBody() {
                return null;
            }

            @Override
            public OutputStream getResponseBody() {
                return null;
            }

            @Override
            public void sendResponseHeaders(int rCode, long responseLength) throws IOException {

            }

            @Override
            public InetSocketAddress getRemoteAddress() {
                return null;
            }

            @Override
            public int getResponseCode() {
                return 0;
            }

            @Override
            public InetSocketAddress getLocalAddress() {
                return null;
            }

            @Override
            public String getProtocol() {
                return null;
            }

            @Override
            public Object getAttribute(String name) {
                return null;
            }

            @Override
            public void setAttribute(String name, Object value) {

            }

            @Override
            public void setStreams(InputStream i, OutputStream o) {

            }

            @Override
            public HttpPrincipal getPrincipal() {
                return null;
            }
        };
    }



}