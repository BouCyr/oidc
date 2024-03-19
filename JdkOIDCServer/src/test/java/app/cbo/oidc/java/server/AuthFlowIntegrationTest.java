package app.cbo.oidc.java.server;

import app.cbo.oidc.java.server.credentials.TOTP;
import app.cbo.oidc.java.server.http.authorize.AuthorizeHandler;
import app.cbo.oidc.java.server.http.jwks.JWKSHandler;
import app.cbo.oidc.java.server.http.token.TokenHandler;
import app.cbo.oidc.java.server.http.userinfo.UserInfoHandler;
import app.cbo.oidc.java.server.scan.exceptions.DownStreamException;
import app.cbo.oidc.java.server.utils.MimeType;
import app.cbo.oidc.java.server.utils.QueryStringParser;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.CookieHandler;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class AuthFlowIntegrationTest {

    private static final String SCHEME = "http://";
    private static final String DOMAIN = "localhost";

    private static final String ROOT(int PORT) {
        return SCHEME + DOMAIN + ":" + PORT;
    }

    static DecodedJWT validateJWT(int PORT, String idToken) throws JOSEException {
        var decoded = JWT.decode(idToken);
        var keyId = decoded.getKeyId();

        JWKSet jwkSet;
        try {
            jwkSet = JWKSet.load(new URL(ROOT(PORT) + JWKSHandler.JWKS_ENDPOINT));
        } catch (Exception e) {
            fail("cannot load JKWSet");
            throw new RuntimeException("wnh");
        }
        var key = jwkSet.getKeys().stream().filter(jwk -> jwk.getKeyID().equals(keyId)).findFirst();
        if (key.isEmpty()) {
            fail("Cannot find signature key in keySet");
            throw new RuntimeException("wnh");
        }
        var publicKey = key.get();

        if (publicKey instanceof RSAKey rsaKey) {
            var javaKey = (RSAPublicKey) rsaKey.toPublicKey();
            JWT.require(Algorithm.RSA256(javaKey)).build().verify(decoded);
        }
        assertThat(decoded).isNotNull();
        return decoded;
    }

    static URI submitConsent(int PORT, HttpClient browser, HttpResponse<String> consentPage, String consentOngoing) throws IOException, InterruptedException, URISyntaxException {
        var consentRequest = HttpRequest.newBuilder()
                .uri(consentPage.uri())
                .header("Content-Type", MimeType.FORM.mimeType())
                .POST(HttpRequest.BodyPublishers.ofString("backFromForm=true&OK=true&ongoing=" + consentOngoing))
                .build();

        URI sentToClient = null;
        try {
            followRedirects(PORT, browser, consentRequest);
            fail("Should have been redirected");
        } catch (OutsideRedirect e) {
            sentToClient = e.redirectTo();
        }
        assertThat(sentToClient)
                .isNotNull()
                .hasAuthority("cbo.app");


        return sentToClient;
    }

    static HttpResponse<String> submitCredentialsAndRedirectToConsents(int PORT, String userName, HttpClient browser, HttpResponse<String> loginPage) throws IOException, InterruptedException, URISyntaxException, OutsideRedirect {
        var totp = TOTP.get("ALBACORE"); //[05/04/2023] should you an external way to generate TOTP
        var loginOngoings = QueryStringParser.from(loginPage.uri().getQuery()).get("ongoing");
        assertThat(loginOngoings)
                .isNotNull()
                .hasSize(1);
        var loginOngoing = loginOngoings.iterator().next();


        var encodedUri = new URI(loginPage.uri().toString().replace(" ", "%20"));
        var authenticationRequest = HttpRequest.newBuilder()
                .uri(encodedUri)
                .header("Content-Type", MimeType.FORM.mimeType())
                .POST(HttpRequest.BodyPublishers.ofString("login=" + userName + "&pwd=sesame&totp=" + totp + "&ongoing=" + loginOngoing))
                .build();

        return followRedirects(PORT, browser, authenticationRequest);
    }

    static String readOngoingInputField(String html) {

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
    static HttpResponse<String> followRedirects(int PORT, HttpClient browser, HttpRequest req) throws IOException, InterruptedException, URISyntaxException, OutsideRedirect {
        var res = browser.send(req, ofString());

        var status = res.statusCode();
        var locationHeader = res.headers().firstValue("location");
        if (status / 100 == 3 && locationHeader.isPresent()) {

            var target = locationHeader.get();

            //fix relative redirects
            var redirectedTo = target.startsWith("http") ? new URI(target.replace(" ", "%20")) : new URI((ROOT(PORT) + locationHeader.get()).replace(" ", "%20"));

            if (!(DOMAIN + ":" + PORT).equals(redirectedTo.getAuthority())) {
                throw new OutsideRedirect("redirect outside of test scope!", redirectedTo);
            }
            var redirectedRequest = HttpRequest.newBuilder()
                    .uri(redirectedTo)
                    .GET().build();
            res = followRedirects(PORT, browser, redirectedRequest);
        }
        return res;
    }

    @Test
    public void authorizationFlow() throws IOException, URISyntaxException, InterruptedException, OutsideRedirect, JOSEException, DownStreamException {
        int PORT = 4546;
        EntryPoint.main("profile=mem",
                "port=" + PORT,
                "backend=mem",
                "scanner="+TestScanner.class.getCanonicalName());


        var cookies = new MyCookies();
        var browser = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .cookieHandler(cookies)
                .build();


        HttpResponse<String> loginPage = callAuthorizeAndRedirectToLogin(PORT, browser);
        HttpResponse<String> consentPage = submitCredentialsAndRedirectToConsents(PORT, "cyrille", browser, loginPage);

        String consentOngoing = readOngoingInputField(consentPage.body());
        assertThat(consentPage.statusCode()).isEqualTo(200);

        var code = submitConsentAndRedirectToClientWithCode(PORT, browser, consentPage, consentOngoing);


        JsonNode json = submitCode(PORT, browser, code);

        var accessToken = json.get("access_token");
        var refreshToken = json.get("refresh_token");
        var idToken = json.get("id_token");

        assertThat(accessToken).isNotNull();
        assertThat(refreshToken).isNotNull();
        assertThat(idToken).isNotNull();

        DecodedJWT decoded = validateJWT(PORT, idToken.asText());


        assertThat(decoded.getSubject()).isEqualTo("cyrille");
        var userInfoRequest = HttpRequest.newBuilder()
                .uri(new URI(ROOT(PORT) + UserInfoHandler.USERINFO_ENDPOINT))
                .header("Content-Type", MimeType.FORM.mimeType())
                .header("Authorization", "Bearer " + accessToken.asText())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        var userInfoResponse = browser.send(userInfoRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(userInfoResponse).isNotNull();
        assertThat(userInfoResponse.statusCode()).isEqualTo(200);
        var userInfo = new ObjectMapper().reader().readTree(userInfoResponse.body());


        assertThat(userInfo).isNotNull();
        assertThat(userInfo.get("name").asText()).isEqualToIgnoringCase("cyrille boucher");
        assertThat(userInfo.get("phone")).isNull(); //did not ask for 'phone' scope

    }

    private JsonNode submitCode(int PORT, HttpClient browser, String code) throws URISyntaxException, IOException, InterruptedException {
        var codeReq = HttpRequest.newBuilder()
                .uri(new URI(ROOT(PORT) + TokenHandler.TOKEN_ENDPOINT))
                .header("Content-Type", MimeType.FORM.mimeType())
                .header("Authorization", "basic " + Base64.getEncoder().encodeToString("INTEGRATION:INTEGRATION".getBytes(StandardCharsets.UTF_8)))
                .POST(HttpRequest.BodyPublishers.ofString("code=" + code + "&grant_type=authorization_code&client_id=INTEGRATION&redirect_uri=http://cbo.app"))
                .build();
        var codeRes = browser.send(codeReq, ofString());
        var codeStatus = codeRes.statusCode();
        assertThat(codeStatus).isEqualTo(200);

        var tokenResponse = codeRes.body();

        return new ObjectMapper().reader().readTree(tokenResponse);
    }

    private String submitConsentAndRedirectToClientWithCode(int PORT, HttpClient browser, HttpResponse<String> consentPage, String consentOngoing) throws IOException, InterruptedException, URISyntaxException {
        URI sentToClient = submitConsent(PORT, browser, consentPage, consentOngoing);
        assertThat(sentToClient)
                .hasParameter("code");


        var codeParam = QueryStringParser.from(sentToClient.getQuery())
                .get("code");

        assertThat(codeParam)
                .isNotNull()
                .isNotEmpty()
                .hasSize(1);

        return codeParam.iterator().next();
    }

    private HttpResponse<String> callAuthorizeAndRedirectToLogin(int PORT, HttpClient browser) throws URISyntaxException, IOException, InterruptedException, OutsideRedirect {
        var authorizeRequest = HttpRequest.newBuilder()
                .uri(new URI(ROOT(PORT) + AuthorizeHandler.AUTHORIZE_ENDPOINT + "?scope=openid%20profile&redirect_uri=http://cbo.app&response_type=code&client_id=INTEGRATION&nonce=monNonce"))
                .GET().build();

        HttpResponse<String> loginPage = followRedirects(PORT, browser, authorizeRequest);

        assertThat(loginPage.statusCode()).isEqualTo(200);
        return loginPage;
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
