package app.cbo.oidc.java.server.http.token;

import app.cbo.oidc.java.server.oidc.tokens.IdToken;
import app.cbo.oidc.java.server.scan.Injectable;

@FunctionalInterface
public interface IdTokenCustomizer {

    IdToken customize(IdToken source);

    @Injectable
    class Noop implements IdTokenCustomizer{
        @Override
        public IdToken customize(IdToken source) {
            return source;
        }
    }
}
