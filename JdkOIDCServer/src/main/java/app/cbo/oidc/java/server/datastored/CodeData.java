package app.cbo.oidc.java.server.datastored;

import java.util.List;

public record CodeData(UserId userId, SessionId sessionId, List<String> scopes) {
}
