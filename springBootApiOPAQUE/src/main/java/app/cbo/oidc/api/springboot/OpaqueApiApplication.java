package app.cbo.oidc.api.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OpaqueApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpaqueApiApplication.class, args);
    }

    @Bean
    public RestTemplateBuilder restTemplateBuilder() {
        return new RestTemplateBuilder(rt -> rt.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("User-agent", "coucou");
            return execution.execute(request, body);
        }));
    }
}
