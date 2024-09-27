package app.cbo.oidc.client.springboot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.stream.Collectors;

@SpringBootApplication
@RestController
public class SpringbootclientApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootclientApplication.class, args);
    }

    private static final Logger LOG = LoggerFactory.getLogger(SpringbootclientApplication.class);

    private static String callApi(OAuth2AccessToken accessToken, String apiUrl) {
        var http = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken.getTokenValue());
        var requestEntity = new RequestEntity<String>(
                headers,
                HttpMethod.GET,
                URI.create(apiUrl)
        );
        var result = http.exchange(requestEntity, String.class);
        return result.getBody();
    }

    @GetMapping("/api")
    public String callApi(@RegisteredOAuth2AuthorizedClient("mine") OAuth2AuthorizedClient authorizedClient) {

        var accessToken = authorizedClient.getAccessToken();

        String jwtResponse, opaqueResponse;
        try {
            opaqueResponse = callApi(accessToken, "http://localhost:9453/ok");
        } catch (Exception e) {
            LOG.info("Exception calling opaque API", e);
            opaqueResponse = e.getMessage();
        }
        try {
            jwtResponse = callApi(accessToken, "http://localhost:9454/ok");
        } catch (Exception e) {
            LOG.info("Exception calling JWT API ", e);
            jwtResponse = e.getMessage();
        }

        return "Opaque : " + opaqueResponse +
                ", JWT : " + jwtResponse;

    }


    @GetMapping(path = "/user", produces = MediaType.TEXT_HTML_VALUE)
    public String user(@AuthenticationPrincipal OidcUser principal,
                       @RegisteredOAuth2AuthorizedClient("mine") OAuth2AuthorizedClient authorizedClient) {

        var accessToken = authorizedClient.getAccessToken();
        var refreshToken = authorizedClient.getRefreshToken();


        return "<h3>id_token</h3>" +
                "<ul>" + principal.getIdToken().getClaims().entrySet()
                .stream()
                .map((kv) -> "<li>" + kv.getKey() + " : " + kv.getValue() + "</li>")
                .collect(Collectors.joining(System.lineSeparator())) + "</ul>" +
                "<h3>userinfo</h3>" +
                "<ul>" + principal.getUserInfo().getClaims().entrySet()
                .stream()
                .map((kv) -> "<li>" + kv.getKey() + " : " + kv.getValue() + "</li>")
                .collect(Collectors.joining(System.lineSeparator())) + "</ul>" +
                "<h3>access_token</h3>" +
                "<p>" + accessToken.getTokenValue() + "</p>" +
                "<h3>refresh_token</h3>" +
                "<p>" + refreshToken.getTokenValue() + "</p>";
    }

}
