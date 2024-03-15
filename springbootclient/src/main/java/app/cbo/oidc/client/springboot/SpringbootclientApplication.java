package app.cbo.oidc.client.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@SpringBootApplication
@RestController
public class SpringbootclientApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootclientApplication.class, args);
    }

    @GetMapping(path = "/user", produces = MediaType.TEXT_HTML_VALUE)
    public String user(@AuthenticationPrincipal OidcUser principal) {


        return "<h3>IdToken</h3>" +
                "<ul>" + principal.getIdToken().getClaims().entrySet()
                .stream()
                .map((kv) -> "<li>" + kv.getKey() + " : " + kv.getValue() + "</li>")
                .collect(Collectors.joining(System.lineSeparator())) + "</ul>" +
                "<h3>userinfo</h3>" +
                "<ul>" + principal.getUserInfo().getClaims().entrySet()
                .stream()
                .map((kv) -> "<li>" + kv.getKey() + " : " + kv.getValue() + "</li>")
                .collect(Collectors.joining(System.lineSeparator())) + "</ul>";
    }

}
