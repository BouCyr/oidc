package app.cbo.oidc.java.server.oidc;

import app.cbo.oidc.java.server.json.WithExtraNode;

import java.util.Map;

public record UserInfo(String sub, Map<String, Object> extranodes) implements WithExtraNode {


}
