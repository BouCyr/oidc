package app.cbo.oidc.java.server.credentials.pwds;

import app.cbo.oidc.java.server.jsr305.NotNull;

public interface PasswordChecker {

    boolean confront(@NotNull String provided, @NotNull String storedEncoded);
}
