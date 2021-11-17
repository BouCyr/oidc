package app.cbo.oidc.toyidc.functions;

import app.cbo.oidc.toyidc.backend.StoredCode;

import java.util.Optional;

@FunctionalInterface
public interface CodeRetriever {

    Optional<StoredCode> retrieve(String providedCode);
}
