package app.cbo.oidc.api.springboot;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.JwkSetUriJwtDecoderBuilderCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;

import java.util.Set;

@SpringBootApplication
public class JwtApiApplication {


    @Value("${spring.application.name")
    private String applicationName;

    public static void main(String[] args) {


        SpringApplication.run(JwtApiApplication.class, args);
    }

    /**
     * Overrides the default verifier for the 'typ' claim of the JOSE header
     * By default, Nimbus does not allow "at+jwt", which is what is specified in RFC 9068.
     * <p>
     * We could also switch behaviour in the IDP server JWTTokenGenerator
     *
     * @return a JOSEObjectTypeVerifier that acct JWT, JOSE, JOSE+JSON <b>AND</b> at+jwt
     */
    @Bean
    JwkSetUriJwtDecoderBuilderCustomizer rfc9068compliance() {
        return builder -> builder.jwtProcessorCustomizer(
                x -> x.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(Set.of(JOSEObjectType.JOSE, JOSEObjectType.JWT, JOSEObjectType.JOSE_JSON, new JOSEObjectType("at+jwt"))
                )));
    }


    @Bean
    RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder().defaultHeader("User-agent", applicationName);
    }

    //WebClientBuilder
}
