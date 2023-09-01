package app.cbo.oidc.java.server.backends.keys;

import app.cbo.oidc.java.server.datastored.KeyId;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jwt.JWKSet;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Optional;

public interface KeySet {
    @NotNull
    JWKSet asJWKSet();

    @NotNull
    KeyId current();

    @NotNull
    Optional<PrivateKey> privateKey(@NotNull KeyId keyId);

    @NotNull
    Optional<PublicKey> publicKey(@NotNull KeyId keyId);

    @NotNull
    KeyId rotate();
}
