package app.cbo.oidc.java.server.http;

import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.scan.Prop;

@Injectable("KEYCLOAK")
public class KeycloakPathCustomizer implements PathCustomizer {


    private final String realmName;

    public KeycloakPathCustomizer(@Prop(value="realm", or="realm") String realmName) {
        this.realmName = realmName;
    }

    public String customize(String basePath){
        return "/realms/"+realmName+basePath;
    }
}
