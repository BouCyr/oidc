package app.cbo.oidc.java.server.datastored;

import app.cbo.oidc.java.server.datastored.user.UserId;

import java.util.List;

public record CodeData(UserId userId, SessionId sessionId, String resource, List<String> scopes, String nonce) {
}
