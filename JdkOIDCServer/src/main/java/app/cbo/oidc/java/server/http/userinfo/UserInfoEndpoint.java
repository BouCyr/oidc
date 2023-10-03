package app.cbo.oidc.java.server.http.userinfo;

import app.cbo.oidc.java.server.http.Interaction;
import app.cbo.oidc.java.server.jsr305.NotNull;

@FunctionalInterface
public interface UserInfoEndpoint {

    @NotNull
    Interaction treatRequest(String accessToken);
}
