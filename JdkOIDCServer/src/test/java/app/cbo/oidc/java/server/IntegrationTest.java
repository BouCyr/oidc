package app.cbo.oidc.java.server;

import app.cbo.oidc.java.server.credentials.TOTP;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeHandler;
import app.cbo.oidc.java.server.endpoints.token.TokenHandler;
import app.cbo.oidc.java.server.utils.MimeType;
import app.cbo.oidc.java.server.utils.QueryStringParser;
import com.auth0.jwt.JWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.assertj.core.api.Assertions.assertThat;

public class IntegrationTest {

    private static final String PORT = "4545";
    private static final String SCHEME = "http://";
    private static final String DOMAIN = "localhost";
    private static final String ROOT = SCHEME + DOMAIN + ":" + PORT;


    @Test
    public void authorizationFlow() throws IOException, URISyntaxException, InterruptedException, OutsideRedirect {
        EntryPoint.main("port=" + PORT);


        var cookies = new MyCookies();
        var browser = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .cookieHandler(cookies)
                .build();


        var authorizeRequest = HttpRequest.newBuilder()
                .uri(new URI(ROOT + AuthorizeHandler.AUTHORIZE_ENDPOINT + "?scope=openid&redirect_uri=http://cbo.app&response_type=code&client_id=INTEGRATION&nonce=monNonce"))
                .GET().build();

        HttpResponse<String> loginPage = followRedirects(browser, authorizeRequest);

        assertThat(loginPage.statusCode()).isEqualTo(200);
        var totp = TOTP.get("ALBACORE"); //[05/04/2023] should you an external way to generate TOTP
        var loginOngoings = QueryStringParser.from(loginPage.uri().getQuery()).get("ongoing");
        assertThat(loginOngoings)
                .isNotNull()
                .hasSize(1);
        var loginOngoing = loginOngoings.iterator().next();


        var authenticationRequest = HttpRequest.newBuilder()
                .uri(loginPage.uri())
                .header("Content-Type", MimeType.FORM.mimeType())
                .POST(HttpRequest.BodyPublishers.ofString("login=cyrille&pwd=sesame&totp=" + totp + "&ongoing=" + loginOngoing))
                .build();

        var consentPage = followRedirects(browser, authenticationRequest);

        String consentOngoing = readOngoingInputField(consentPage.body());
        assertThat(consentPage.statusCode()).isEqualTo(200);

        assertThat(consentOngoing).isNotEqualTo(loginOngoing);

        var consentRequest = HttpRequest.newBuilder()
                .uri(consentPage.uri())
                .header("Content-Type", MimeType.FORM.mimeType())
                .POST(HttpRequest.BodyPublishers.ofString("backFromForm=true&scope_openid=on&scope_profile=on&ongoing=" + consentOngoing))
                .build();

        URI sentToclient = null;
        try {
            var codeSentToClient = followRedirects(browser, consentRequest);

        } catch (OutsideRedirect e) {
            sentToclient = e.redirectTo();
        }

        assertThat(sentToclient)
                .isNotNull()
                .hasAuthority("cbo.app")
                .hasParameter("code");

        var codeParam = QueryStringParser.from(sentToclient.getQuery())
                .get("code");

        assertThat(codeParam)
                .isNotNull()
                .isNotEmpty()
                .hasSize(1);
        var code = codeParam.iterator().next();
        var codeReq = HttpRequest.newBuilder()
                .uri(new URI(ROOT + TokenHandler.TOKEN_ENDPOINT))
                .header("Content-Type", MimeType.FORM.mimeType())
                .header("Authorization", "basic " + Base64.getEncoder().encodeToString("INTEGRATION:INTEGRATION".getBytes(StandardCharsets.UTF_8)))
                .POST(HttpRequest.BodyPublishers.ofString("code=" + code + "&grant_type=authorization_code&client_id=INTEGRATION&redirect_uri=http://cbo.app"))
                .build();
        var codeRes = browser.send(codeReq, ofString());

        var codeStatus = codeRes.statusCode();

        var tokenResponse = codeRes.body();
        var json = new ObjectMapper().reader().readTree(tokenResponse);
        var accessToken = json.get("access_token");
        var refreshToken = json.get("refresh_token");
        var idToken = json.get("id_token");

        assertThat(accessToken).isNotNull();
        assertThat(refreshToken).isNotNull();
        assertThat(idToken).isNotNull();

        var decoded = JWT.decode(idToken.asText());
        var keyId = decoded.getKeyId();

        //TODO call jwks endpoint instead



        /*var pub = (RSAPublicKey) KeySet.getInstance().publicKey(KeyId.of(keyId)).orElseThrow();
        var priv = (RSAPrivateKey) KeySet.getInstance().privateKey(KeyId.of(keyId)).orElseThrow();
        JWT.require(Algorithm.RSA256(pub, priv))
                .build().verify(decoded);*/

        assertThat(decoded.getSubject()).isEqualTo("cyrille");
        //TODO : calls userinfo endpoint
    }

    private String readOngoingInputField(String html) {

        //<input type='hidden' name='ongoing' value='ee736adf-e95c-46b6-afa2-d7c49efeb2ec' />
        var ongoingInputIndex = html.indexOf("<input type='hidden' name='ongoing' value='");
        var ongoingField = html.substring(ongoingInputIndex);
        ongoingField = ongoingField.substring(ongoingField.indexOf("value='") + "value='".length());
        ongoingField = ongoingField.substring(0, ongoingField.indexOf("'"));
        return ongoingField;
    }

    /**
     * Cannot find a way to have the java.net.httpClient follow relative redirects...
     */
    private HttpResponse<String> followRedirects(HttpClient browser, HttpRequest req) throws IOException, InterruptedException, URISyntaxException, OutsideRedirect {
        var res = browser.send(req, ofString());

        var status = res.statusCode();
        var locationHeader = res.headers().firstValue("location");
        if (status / 100 == 3 && locationHeader.isPresent()) {

            var target = locationHeader.get();

            //fix relative redirects
            var redirectedTo = target.startsWith("http") ? new URI(target) : new URI(ROOT + locationHeader.get());

            if (!(DOMAIN + ":" + PORT).equals(redirectedTo.getAuthority())) {
                throw new OutsideRedirect("redirect outside of test scope!", redirectedTo);
            }
            var redirectedRequest = HttpRequest.newBuilder()
                    .uri(redirectedTo)
                    .GET().build();
            res = followRedirects(browser, redirectedRequest);
        }
        return res;
    }


    static class OutsideRedirect extends Exception {
        private final URI redirectTo;

        public OutsideRedirect(String message, URI redirectTo) {
            super(message);
            this.redirectTo = redirectTo;
        }

        public URI redirectTo() {
            return this.redirectTo;
        }
    }

    static class MyCookies extends CookieHandler {

        String sessionId = null;

        @Override
        public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) {

            if (sessionId != null)
                return Map.of("Cookie", List.of(sessionId));
            return Collections.emptyMap();
        }

        @Override
        public void put(URI uri, Map<String, List<String>> responseHeaders) {

            if (responseHeaders.containsKey("set-cookie")) {
                responseHeaders.get("set-cookie")
                        .stream()
                        .filter(s -> s.startsWith("sessionId="))
                        .findAny().ifPresent(s -> this.sessionId = s);
            }

        }
    }
}
