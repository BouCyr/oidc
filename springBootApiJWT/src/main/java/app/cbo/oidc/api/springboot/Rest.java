package app.cbo.oidc.api.springboot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class Rest {

    private static final Logger LOG = LoggerFactory.getLogger(Rest.class);

    @GetMapping("/ok")
    public ResponseEntity<String> validated(@AuthenticationPrincipal Principal principal) {

        String authClass = SecurityContextHolder.getContext().getAuthentication().getClass().getSimpleName();
        String principalName = SecurityContextHolder.getContext().getAuthentication().getPrincipal().getClass().getSimpleName();
        String userName = SecurityContextHolder.getContext().getAuthentication().getName();
        LOG.info("Someone called me");
        return ResponseEntity.ok("%s AUTHENTICATED using %s/%s".formatted(userName, authClass, principalName));
    }


}
