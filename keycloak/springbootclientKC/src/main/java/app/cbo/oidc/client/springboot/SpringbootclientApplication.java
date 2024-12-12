package app.cbo.oidc.client.springboot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@SpringBootApplication
@RestController
public class SpringbootclientApplication {

    private static final Logger LOG = LoggerFactory.getLogger(SpringbootclientApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringbootclientApplication.class, args);
    }

    private static String treatCallback(OidcUser principal, OAuth2AuthorizedClient authorizedClient) {
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

    /**
     * Duplication to match FranceConnect registered callbacks
     *
     * @param principal
     * @param authorizedClient
     * @return
     */
    @GetMapping(path = "/callback", produces = MediaType.TEXT_HTML_VALUE)
    public String fc(@AuthenticationPrincipal OidcUser principal,
                     @RegisteredOAuth2AuthorizedClient("mine") OAuth2AuthorizedClient authorizedClient) {

        return treatCallback(principal, authorizedClient);
    }

    @GetMapping(path = "/user", produces = MediaType.TEXT_HTML_VALUE)
    public String user(@AuthenticationPrincipal OidcUser principal,
                       @RegisteredOAuth2AuthorizedClient("mine") OAuth2AuthorizedClient authorizedClient) {

        return treatCallback(principal, authorizedClient);
    }

}
