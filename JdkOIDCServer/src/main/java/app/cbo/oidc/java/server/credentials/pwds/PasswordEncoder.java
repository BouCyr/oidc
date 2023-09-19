package app.cbo.oidc.java.server.credentials.pwds;

import app.cbo.oidc.java.server.jsr305.NotNull;

public interface PasswordEncoder {

    String encode(@NotNull String clear);
}
