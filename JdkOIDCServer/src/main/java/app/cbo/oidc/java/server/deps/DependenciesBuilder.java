package app.cbo.oidc.java.server.deps;

import app.cbo.oidc.java.server.HttpHandlerWithPath;
import app.cbo.oidc.java.server.NotFoundHandler;
import app.cbo.oidc.java.server.Server;
import app.cbo.oidc.java.server.StartupArgs;
import app.cbo.oidc.java.server.StaticResourceHandler;
import app.cbo.oidc.java.server.backends.claims.Claims;
import app.cbo.oidc.java.server.backends.claims.FSClaims;
import app.cbo.oidc.java.server.backends.claims.MemClaims;
import app.cbo.oidc.java.server.backends.codes.MemCodes;
import app.cbo.oidc.java.server.backends.filesystem.FileStorage;
import app.cbo.oidc.java.server.backends.keys.KeySet;
import app.cbo.oidc.java.server.backends.keys.MemKeySet;
import app.cbo.oidc.java.server.backends.ongoingAuths.OngoingAuths;
import app.cbo.oidc.java.server.backends.sessions.Sessions;
import app.cbo.oidc.java.server.backends.users.FSUsers;
import app.cbo.oidc.java.server.backends.users.MemUsers;
import app.cbo.oidc.java.server.backends.users.Users;
import app.cbo.oidc.java.server.credentials.pwds.PBKDF2WithHmacSHA1PasswordHash;
import app.cbo.oidc.java.server.credentials.pwds.Passwords;
import app.cbo.oidc.java.server.endpoints.authenticate.AuthenticateEndpoint;
import app.cbo.oidc.java.server.endpoints.authenticate.AuthenticateHandler;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeEndpoint;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeHandler;
import app.cbo.oidc.java.server.endpoints.config.ConfigHandler;
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
                this.configHandler(),
                this.notFoundHandler()//should be LAST
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

    public ConfigHandler configHandler() {
        return this.getInstance(ConfigHandler.class,
                () -> new ConfigHandler(
                        //TODO [01/09/2023] domain & protocol
                        "http://localhost:" + args.port() + this.authorizeHandler().path(),
                        "http://localhost:" + args.port() + this.tokenHandler().path(),
                        "http://localhost:" + args.port() + this.userInfoHandler().path(),
                        "http://localhost:" + args.port() + "/logout",
                        "http://localhost:" + args.port() + this.jwksHandler().path()
                ));
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
                        this.users(),
                        this.users()
                ));
    }


    public AuthenticateEndpoint authenticateEndpoint() {
        return this.getInstance(AuthenticateEndpoint.class,
                () -> new AuthenticateEndpoint(
                        this.ongoingAuths(),
                        this.users(),
                        this.sessions(),
                        this.passwords()));
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

    public Users users() {
        Users val;
        if (args.fsBackEnd()) {
            val = this.getInstance(
                    FSUsers.class,
                    () -> new FSUsers(this.userDataFileStorage(), this.passwords()));
        } else {
            val = this.getInstance(MemUsers.class,
                    () -> new MemUsers(this.passwords()));
        }
        return val;
    }

    public MemCodes codes() {
        return this.getInstance(MemCodes.class,
                MemCodes::new);
    }

    public Claims claims() {
        if (args.fsBackEnd()) {
            return this.getInstance(
                    FSClaims.class,
                    () -> new FSClaims(this.userDataFileStorage()));
        } else {
            return this.getInstance(MemClaims.class,
                    MemClaims::new);
        }
    }

    public KeySet keyset() {
        return this.getInstance(MemKeySet.class, MemKeySet::new);
    }

    public FileStorage userDataFileStorage() {
        return this.getInstance(FileStorage.class, () -> {
            try {
                return new FileStorage(args.basePath());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }

    public Passwords passwords() {
        return this.getInstance(PBKDF2WithHmacSHA1PasswordHash.class, PBKDF2WithHmacSHA1PasswordHash::new);
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
