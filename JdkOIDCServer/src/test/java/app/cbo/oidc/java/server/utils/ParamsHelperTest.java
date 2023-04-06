package app.cbo.oidc.java.server.utils;

import app.cbo.oidc.java.server.endpoints.AuthErrorInteraction;
import com.sun.net.httpserver.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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


            try {
                int i = 0;
                System.out.println("@" + (i++));
                Map<String, Collection<String>> params;
                try {
                    System.out.println("@" + (i++));
                    params = ParamsHelper.extractParams(hx);
                    System.out.println("@" + (i++));

                } catch (AuthErrorInteraction authError) {
                    authError.printStackTrace();
                    Assertions.fail("Should work");
                    throw new RuntimeException(authError);
                }

                Assertions.assertThat(params)
                        .hasSize(2)
                        .containsKey("singleParam")
                        .containsKey("double");

                Assertions.assertThat(params.get("singleParam"))
                        .hasSize(1)
                        .element(0).isEqualTo("single");

                Assertions.assertThat(ParamsHelper.singleParam(params.get("singleParam")))
                        .isPresent()
                        .get().isEqualTo("single");

                Assertions.assertThat(ParamsHelper.spaceSeparatedList(ParamsHelper.singleParam(params.get("double")).orElse("")))
                        .hasSize(2)
                        .element(1).isEqualTo("two");


                System.out.println("Receiving request");
                hx.sendResponseHeaders(200, 0);
                hx.close();
                System.out.println("Sending response");
            }catch (AssertionError a){
                hx.sendResponseHeaders(500, 0);
                hx.getResponseBody().write(a.getMessage().getBytes(StandardCharsets.UTF_8));
                hx.close();
                System.out.println("Sending response");
            }
        });

        var c = HttpClient.newHttpClient();

        {//GET
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri("/test?singleParam=single&double=one%20two"))
                    .GET().build();
            System.out.println("Sending request");
            var h = c.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Checking response");

            if (h.statusCode() == 500) {
                Assertions.fail(h.body());
            }
            Assertions.assertThat(h.statusCode()).isEqualTo(200);
        }

        {//POST
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri("/test"))
                    .header("Content-Type", MimeType.FORM.mimeType())
                    .POST(HttpRequest.BodyPublishers.ofString("singleParam=single&double=one two")).build();
            System.out.println("Sending request");
            var h = c.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Checking response");

            if (h.statusCode() == 500) {
                Assertions.fail(h.body());
            }
            Assertions.assertThat(h.statusCode()).isEqualTo(200);
        }

        {//WRONG Content-Type
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri("/test"))
                    .header("Content-Type", MimeType.TEXT_PLAIN.mimeType())
                    .POST(HttpRequest.BodyPublishers.ofString("singleParam=single&double=one two")).build();
            System.out.println("Sending request");
            var h = c.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Checking response");

            Assertions.assertThat(h.statusCode()).isEqualTo(500);
        }
        {//WRONG VERB
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri("/test"))
                    .header("Content-Type", MimeType.TEXT_PLAIN.mimeType())
                    .DELETE().build();
            System.out.println("Sending request");
            var h = c.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Checking response");

            Assertions.assertThat(h.statusCode()).isEqualTo(500);
        }


    }





}