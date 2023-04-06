package app.cbo.oidc.java.server;

import app.cbo.oidc.java.server.credentials.TOTP;
import app.cbo.oidc.java.server.utils.MimeType;
import app.cbo.oidc.java.server.utils.QueryStringParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.net.http.HttpResponse.BodyHandlers.*;
import static org.assertj.core.api.Assertions.*;

public class IntegrationTest {

    @Test
    public void test() throws IOException, URISyntaxException, InterruptedException {
        EntryPoint.main("port=4545");


        var cookies = new MyCookies();
        var browser = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .cookieHandler(cookies)
                .build();
        var authorizeRequest = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:4545/authorize?scope=openid&redirect_uri=http://toto&response_type=code&client_id=INTEGRATION"))
                    .GET().build();

        var redirectionToLogin = browser.send(authorizeRequest, ofString());

        assertThat(redirectionToLogin.statusCode()).isEqualTo(302);
        assertThat(redirectionToLogin.headers().firstValue("location"))
                .isPresent();
        var redirectToLoginURL = redirectionToLogin.headers().firstValue("location").get();


        var loginURI = new URI("http://localhost:4545" + redirectToLoginURL);
        var redirectToLoginRequest = HttpRequest.newBuilder()
                .uri(loginURI)
                .GET().build();

        var loginPage = browser.send(redirectToLoginRequest, ofString() );

        assertThat(loginPage.statusCode()).isEqualTo(200);

        var totp = TOTP.get("ALBACORE"); //[05/04/2023] should you an external way to generate TOTP

        var ongoings = QueryStringParser.from(loginURI.getQuery()).get("ongoing");
        assertThat(ongoings).isNotNull().hasSize(1);


        var authenticationRequest = HttpRequest.newBuilder()
                .uri(loginURI)
                .header("Content-Type", MimeType.FORM.mimeType())
                .POST( HttpRequest.BodyPublishers.ofString("login=cyrille&pwd=sesame&totp="+totp+"&ongoing="+ongoings.iterator().next()))
                .build();

        var redirectionToAuthorize = browser.send(authenticationRequest, ofString());

        assertThat(redirectionToAuthorize.statusCode()).isEqualTo(302);

        var authorizeWithAuthentRequest = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:4545"+redirectionToAuthorize.headers().firstValue("location").get()))
                .build();


        var redirectToConsent = browser.send(authorizeWithAuthentRequest, ofString());

        redirectToConsent.toString();

    }

    static class MyCookies extends CookieHandler {

        String sessionId = null;
        @Override
        public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {

            if(sessionId != null)
                return Map.of("Cookie", List.of(sessionId));
            return Collections.emptyMap();
        }

        @Override
        public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {

            if(responseHeaders.containsKey("set-cookie"))
            {
                responseHeaders.get("set-cookie")
                        .stream()
                        .filter(s -> s.startsWith("sessionId="))
                        .findAny().ifPresent(s -> this.sessionId=s);
            }

        }
    }
}
