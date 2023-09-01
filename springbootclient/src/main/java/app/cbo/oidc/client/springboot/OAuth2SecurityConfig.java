package app.cbo.oidc.client.springboot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OAuth2SecurityConfig {

    @Value("${oidc.client-id}")
    private String clientId;

    @Value("${oidc.client-secret}")
    private String clientSecret;

    @Value("${oidc.base-url}")
    private String baseUrl;

   /* @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(this.keycloakClientRegistration());
    }

    private ClientRegistration keycloakClientRegistration() {
        return ClientRegistration.withRegistrationId("clientweb")
                .clientId(this.clientId)
                .clientSecret(this.clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "profile", "email", "address", "phone")
                .clientName("sb")
                .providerConfigurationMetadata().issuerUri()

                .issuerUri(this.baseUrl)
//                .authorizationUri(this.baseUrl + "/authorize")
//                .tokenUri(this.baseUrl + "/token")
//                .jwkSetUri(this.baseUrl + "/jwks")
//                .userInfoUri(this.baseUrl + "/userinfo")
                .userNameAttributeName("sub")
                .build();
    }*/

}
