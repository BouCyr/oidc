package app.cbo.oidc.java.server;

import app.cbo.oidc.java.server.http.authorize.AuthorizeHandler;
import app.cbo.oidc.java.server.http.userinfo.UserInfoHandler;
import app.cbo.oidc.java.server.scan.exceptions.DownStreamException;
import app.cbo.oidc.java.server.utils.MimeType;
import app.cbo.oidc.java.server.utils.QueryStringParser;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

public class ImplicitFlowIntegrationTest {

    private static final String SCHEME = "http://";
    private static final String DOMAIN = "localhost";

    private static String ROOT(int PORT) {
        return SCHEME + DOMAIN + ":" + PORT;
    }


    @BeforeAll
    static void startup() {

    }

    @Test
    @Disabled
    void implicitFlowWithoutAccessToken() throws IOException, URISyntaxException, InterruptedException, AuthFlowIntegrationTest.OutsideRedirect, JOSEException, DownStreamException {

        int PORT = 4547;
        EntryPoint.main("port=" + PORT, "backend=mem");

        var cookies = new AuthFlowIntegrationTest.MyCookies();
        var browser = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .cookieHandler(cookies)
                .build();

        HttpResponse<String> loginPage = this.callAuthorizeForIdTokenAndRedirectToLogin(PORT, browser);
        HttpResponse<String> consentPage = AuthFlowIntegrationTest.submitCredentialsAndRedirectToConsents(PORT, "cyrille", browser, loginPage);

        String consentOngoing = AuthFlowIntegrationTest.readOngoingInputField(consentPage.body());
        assertThat(consentPage.statusCode()).isEqualTo(200);

        URI sentToClient = AuthFlowIntegrationTest.submitConsent(PORT, browser, consentPage, consentOngoing);

        var fragmentPart = QueryStringParser.from(sentToClient.getFragment());

        assertThat(fragmentPart).doesNotContainKey("access_token")
                .doesNotContainKey("expires_in");

        var idToken = fragmentPart.get("id_token").iterator().next();

        assertThat(idToken).isNotNull();

        DecodedJWT decoded = AuthFlowIntegrationTest.validateJWT(PORT, idToken);

        assertThat(decoded.getSubject()).isEqualTo("cyrille");
        //since we do not have requested access token, scoped claims should be in the id_token
        assertThat(decoded.getClaim("email").asString())
                .isNotNull()
                .isEqualTo("cyrille@example.com");

    }

    @Test
    @Disabled
    void implicitFlowWithAccessToken() throws IOException, URISyntaxException, InterruptedException, AuthFlowIntegrationTest.OutsideRedirect, JOSEException, DownStreamException {

        int PORT = 4548;
        EntryPoint.main("port=" + PORT, "backend=mem");

        var cookies = new AuthFlowIntegrationTest.MyCookies();
        var browser = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .cookieHandler(cookies)
                .build();

        HttpResponse<String> loginPage = callAuthorizeForBothTokensAndRedirectToLogin(PORT, browser);
        HttpResponse<String> consentPage = AuthFlowIntegrationTest.submitCredentialsAndRedirectToConsents(PORT, "caroline", browser, loginPage);

        String consentOngoing = AuthFlowIntegrationTest.readOngoingInputField(consentPage.body());
        assertThat(consentPage.statusCode()).isEqualTo(200);

        URI sentToClient = AuthFlowIntegrationTest.submitConsent(PORT, browser, consentPage, consentOngoing);

        var fragmentPart = QueryStringParser.from(sentToClient.getFragment());

        var accessToken = fragmentPart.get("access_token").iterator().next();
        var idToken = fragmentPart.get("id_token").iterator().next();

        assertThat(accessToken).isNotNull();
        assertThat(idToken).isNotNull();

        DecodedJWT decoded = AuthFlowIntegrationTest.validateJWT(PORT, idToken);

        assertThat(decoded.getSubject()).isEqualTo("caroline");
        var userInfoRequest = HttpRequest.newBuilder()
                .uri(new URI(ROOT(PORT) + UserInfoHandler.USERINFO_ENDPOINT))
                .header("Content-Type", MimeType.FORM.mimeType())
                .header("Authorization", "Bearer " + accessToken)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        var userInfoResponse = browser.send(userInfoRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(userInfoResponse).isNotNull();
        assertThat(userInfoResponse.statusCode()).isEqualTo(200);
        var userInfo = new ObjectMapper().reader().readTree(userInfoResponse.body());


        assertThat(userInfo).isNotNull();
        assertThat(userInfo.get("name").asText()).isEqualToIgnoringCase("caroline boucher");
        assertThat(userInfo.get("phone")).isNull(); //did not ask for 'phone' scope
        //did ask for email scope ; since we asked for a 'token', email info should be in userInfo...
        assertThat(userInfo.get("email")).isNotNull();
        assertThat(userInfo.get("email").asText()).isEqualTo("caroline@example.com");
        //...and not in id_token
        assertThat(decoded.getClaim("email").asString()).isNull();

    }

    private HttpResponse<String> callAuthorizeForIdTokenAndRedirectToLogin(int PORT, HttpClient browser) throws URISyntaxException, IOException, InterruptedException, AuthFlowIntegrationTest.OutsideRedirect {
        var authorizeRequest = HttpRequest.newBuilder()
                .uri(new URI(ROOT(PORT) + AuthorizeHandler.AUTHORIZE_ENDPOINT + "?scope=openid%20profile%20email" +
                        //we only ask for token_id
                        "&redirect_uri=http://cbo.app&response_type=id_token&client_id=INTEGRATION&nonce=monNonce"))
                .GET().build();

        HttpResponse<String> loginPage = AuthFlowIntegrationTest.followRedirects(PORT, browser, authorizeRequest);

        assertThat(loginPage.statusCode()).isEqualTo(200);
        return loginPage;
    }

    private HttpResponse<String> callAuthorizeForBothTokensAndRedirectToLogin(int PORT, HttpClient browser) throws URISyntaxException, IOException, InterruptedException, AuthFlowIntegrationTest.OutsideRedirect {
        var authorizeRequest = HttpRequest.newBuilder()
                .uri(new URI(ROOT(PORT) + AuthorizeHandler.AUTHORIZE_ENDPOINT + "?scope=openid%20profile%20email" +
                        //we ask for access AND id tokens
                        "&redirect_uri=http://cbo.app&response_type=token%20id_token&client_id=INTEGRATION&nonce=monNonce"))
                .GET().build();

        HttpResponse<String> loginPage = AuthFlowIntegrationTest.followRedirects(PORT, browser, authorizeRequest);

        assertThat(loginPage.statusCode()).isEqualTo(200);
        return loginPage;
    }
}
