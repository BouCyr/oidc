package app.cbo.oidc.client.springboot;

import com.squareup.okhttp.ConnectionPool;
import com.squareup.okhttp.OkHttpClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.security.cert.X509Certificate;
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

    @Bean
    public RestTemplate okhttp3Template() {
        RestTemplate restTemplate = new RestTemplate();

        Neo4jProperties.Security.TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

        ConnectionPool sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();

        SSLConnectionSocketFactory csf = new SSLConnectionSocketFactory(sslContext);

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        ConnectionPool okHttpConnectionPool = new ConnectionPool(HTTP_MAX_IDLE, HTTP_KEEP_ALIVE,
                TimeUnit.SECONDS);
        builder.connectionPool(okHttpConnectionPool);
        builder.connectTimeout(HTTP_CONNECTION_TIMEOUT, TimeUnit.SECONDS);
        builder.retryOnConnectionFailure(false);

        restTemplate.setRequestFactory(new OkHttp3ClientHttpRequestFactory(builder.build()));

        return restTemplate;
    }
}
