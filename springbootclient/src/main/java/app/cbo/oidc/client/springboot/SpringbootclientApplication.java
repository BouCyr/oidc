package app.cbo.oidc.client.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
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


    @GetMapping("/api")
    public String callApi(@RegisteredOAuth2AuthorizedClient("mine") OAuth2AuthorizedClient authorizedClient) {

        var accessToken = authorizedClient.getAccessToken();

        var http = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken.getTokenValue());
        var requestEntity = new RequestEntity<String>(
                headers,
                HttpMethod.GET,
                URI.create("http://localhost:9453/ok")
        );
        var result = http.exchange(requestEntity, String.class);
        return result.getBody();

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
