package app.cbo.oidc.java.server.deps;

import app.cbo.oidc.java.server.*;
import app.cbo.oidc.java.server.backends.KeySet;
import app.cbo.oidc.java.server.backends.claims.MemClaims;
import app.cbo.oidc.java.server.backends.codes.Codes;
import app.cbo.oidc.java.server.backends.ongoingAuths.OngoingAuths;
import app.cbo.oidc.java.server.backends.sessions.Sessions;
import app.cbo.oidc.java.server.backends.users.MemUsers;
import app.cbo.oidc.java.server.endpoints.authenticate.AuthenticateEndpoint;
import app.cbo.oidc.java.server.endpoints.authenticate.AuthenticateHandler;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeEndpoint;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeHandler;
import app.cbo.oidc.java.server.endpoints.consent.ConsentEndpoint;
import app.cbo.oidc.java.server.endpoints.consent.ConsentHandler;
import app.cbo.oidc.java.server.endpoints.jwks.JWKSHandler;
import app.cbo.oidc.java.server.endpoints.token.TokenEndpoint;
import app.cbo.oidc.java.server.endpoints.token.TokenHandler;
import app.cbo.oidc.java.server.endpoints.userinfo.UserInfoEndpoint;
import app.cbo.oidc.java.server.endpoints.userinfo.UserInfoHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class DependenciesBuilder {

    private final StartupArgs args;

    private final Map<String, Object> context = new ConcurrentHashMap<>();

    public DependenciesBuilder(StartupArgs args) {
        this.args = args;
    }

    public Server server() {
        return getInstance(Server.class, () -> {
            try {
                return new Server(this.args, this.handlers());
            } catch (IOException e) {

                throw new RuntimeException(e);
            }
        });
    }

    public List<HttpHandlerWithPath> handlers() {
        return List.of(
                this.authorizeHandler(),
                this.authenticateHandler(),
                this.consentHandler(),
                this.tokenHandler(),
                this.userInfoHandler(),
                this.jwksHandler(),
                this.staticResourceHandler(),
                this.notFoundHandler()
        );
    }

    public NotFoundHandler notFoundHandler() {
        return this.getInstance(NotFoundHandler.class, NotFoundHandler::new);
    }

    public StaticResourceHandler staticResourceHandler() {
        return this.getInstance(StaticResourceHandler.class, StaticResourceHandler::new);
    }

    public JWKSHandler jwksHandler() {
        return this.getInstance(JWKSHandler.class,
                () -> new JWKSHandler(this.keyset()));
    }


    public UserInfoHandler userInfoHandler() {
        return this.getInstance(UserInfoHandler.class,
                () -> new UserInfoHandler(this.userInfoEndpoint()));
    }

    public TokenHandler tokenHandler() {
        return this.getInstance(TokenHandler.class,
                () -> new TokenHandler(this.tokenEndpoint()));
    }

    public ConsentHandler consentHandler() {
        return this.getInstance(ConsentHandler.class,
                () -> new ConsentHandler(this.ongoingAuths(), this.consentEndpoint(), this.sessions()));
    }

    public AuthenticateHandler authenticateHandler() {
        return this.getInstance(AuthenticateHandler.class,
                () -> new AuthenticateHandler(this.authenticateEndpoint(), this.sessions()));

    }

    public AuthorizeHandler authorizeHandler() {

        return this.getInstance(AuthorizeHandler.class,
                () -> new AuthorizeHandler(this.authorizeEndpoint(), this.sessions()));

    }

    public TokenEndpoint tokenEndpoint() {
        return this.getInstance(TokenEndpoint.class,
                () -> new TokenEndpoint(this.codes(), this.users(), this.sessions(), this.keyset()));
    }

    public UserInfoEndpoint userInfoEndpoint() {
        return this.getInstance(UserInfoEndpoint.class,
                () -> new UserInfoEndpoint(this.claims(), this.keyset()));
    }

    public ConsentEndpoint consentEndpoint() {
        return this.getInstance(ConsentEndpoint.class,
                () -> new ConsentEndpoint(
                        this.ongoingAuths(),
                        this.users()
                ));
    }


    public AuthenticateEndpoint authenticateEndpoint() {
        return this.getInstance(AuthenticateEndpoint.class,
                () -> new AuthenticateEndpoint(
                        this.ongoingAuths(),
                        this.users(),
                        this.sessions()
                ));
    }


    public AuthorizeEndpoint authorizeEndpoint() {
        return this.getInstance(AuthorizeEndpoint.class,
                () -> new AuthorizeEndpoint(
                        this.ongoingAuths(),
                        this.users(),
                        this.codes(),
                        this.keyset(),
                        this.claims()));
    }

    public OngoingAuths ongoingAuths() {
        return this.getInstance(OngoingAuths.class,
                OngoingAuths::new);
    }

    public Sessions sessions() {
        return this.getInstance(Sessions.class,
                Sessions::new);
    }

    public MemUsers users() {
        return this.getInstance(MemUsers.class,
                MemUsers::new);
    }

    public Codes codes() {
        return this.getInstance(Codes.class,
                Codes::new);
    }

    public MemClaims claims() {
        return this.getInstance(MemClaims.class, MemClaims::new);
    }

    public KeySet keyset() {
        return this.getInstance(KeySet.class, KeySet::new);
    }

    private <U> U getInstance(Class<U> clazz, Supplier<U> cr) {

        if (!context.containsKey(clazz.getCanonicalName())) {
            context.put(clazz.getCanonicalName(), cr.get());
        }

        var instance = context.get(clazz.getCanonicalName());
        if (clazz.isAssignableFrom(instance.getClass()))
            return (U) instance;
        throw new IllegalStateException();

    }
}
