package app.cbo.oidc.java.server.http;

import app.cbo.oidc.java.server.scan.Injectable;

@Injectable("KEYCLOAK")
public class KeycloakPathCustomizer implements PathCustomizer {

    public String customize(String basePath){
        return "/realms/%REALMS%"+basePath;
    }
}
