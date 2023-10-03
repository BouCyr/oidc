package app.cbo.oidc.java.server.http.userinfo;

import app.cbo.oidc.java.server.datastored.user.UserId;

import java.util.Set;

public record AccessTokenData(UserId sub, Set<String> scopes) {
}
