package app.cbo.oidc.client.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
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
    public String user(@AuthenticationPrincipal OAuth2User principal) {


        return "<ul>" + principal.getAttributes().entrySet()
                .stream()
                .map((kv) -> "<li>" + kv.getKey() + " : " + kv.getValue() + "</li>")
                .collect(Collectors.joining(System.lineSeparator())) + "</ul>";
    }
}
