package app.cbo.oidc.api.springboot;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class Rest {

    @GetMapping("/ok")
    public ResponseEntity<String> validated(@AuthenticationPrincipal Principal principal) {
        return ResponseEntity.ok("ok");
    }
}
