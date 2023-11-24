package app.cbo.oidc.java.server.deps;

import app.cbo.oidc.java.server.Server;
import app.cbo.oidc.java.server.StartupArgs;
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
import app.cbo.oidc.java.server.http.HttpHandlerWithPath;
import app.cbo.oidc.java.server.http.NotFoundHandler;
import app.cbo.oidc.java.server.http.authenticate.AuthenticateEndpoint;
import app.cbo.oidc.java.server.http.authenticate.AuthenticateEndpointImpl;
import app.cbo.oidc.java.server.http.authenticate.AuthenticateHandler;
import app.cbo.oidc.java.server.http.authorize.AuthorizeEndpoint;
import app.cbo.oidc.java.server.http.authorize.AuthorizeHandler;
import app.cbo.oidc.java.server.http.config.ConfigHandler;
import app.cbo.oidc.java.server.http.consent.ConsentEndpoint;
import app.cbo.oidc.java.server.http.consent.ConsentEndpointImpl;
import app.cbo.oidc.java.server.http.consent.ConsentHandler;
import app.cbo.oidc.java.server.http.jwks.JWKSHandler;
import app.cbo.oidc.java.server.http.staticcontent.StaticResourceHandler;
import app.cbo.oidc.java.server.http.token.TokenEndpoint;
import app.cbo.oidc.java.server.http.token.TokenEndpointImpl;
import app.cbo.oidc.java.server.http.token.TokenHandler;
import app.cbo.oidc.java.server.http.userinfo.AccessTokenValidator;
import app.cbo.oidc.java.server.http.userinfo.JWTAccessTokenValidator;
import app.cbo.oidc.java.server.http.userinfo.UserInfoEndpointImpl;
import app.cbo.oidc.java.server.http.userinfo.UserInfoHandler;
import app.cbo.oidc.java.server.oidc.Issuer;
import app.cbo.oidc.java.server.scan.ProgramArgs;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class DependenciesBuilder {

    private final ProgramArgs args;

    private final Map<String, Object> context = new ConcurrentHashMap<>();

    public DependenciesBuilder(ProgramArgs args) {
        this.args = args;
    }

    public Server server() {
        return getInstance(Server.class, () -> {
            try {
                return new Server(9451, this.handlers());
            } catch (IOException e) {

                throw new RuntimeException(e);
            }
        });
    }

    public Issuer issuerId() {

        int port = Integer.parseInt(this.args.arg("port").orElseThrow());
        return Issuer.of("http://localhost:" + port);

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
                () -> new AuthenticateHandler(this.authenticateEndpoint()));

    }

    public AuthorizeHandler authorizeHandler() {

        return this.getInstance(AuthorizeHandler.class,
                () -> new AuthorizeHandler(this.authorizeEndpoint(), this.sessions()));

    }

    public ConfigHandler configHandler() {
        return this.getInstance(ConfigHandler.class,
                () -> new ConfigHandler(
                        this.issuerId(),
                        this.issuerId().getIssuerId() + this.authorizeHandler().path(),
                        this.issuerId().getIssuerId() + this.tokenHandler().path(),
                        this.issuerId().getIssuerId() + this.userInfoHandler().path(),
                        this.issuerId().getIssuerId() + "/logout",
                        this.issuerId().getIssuerId() + this.jwksHandler().path()
                ));
    }


    public TokenEndpoint tokenEndpoint() {
        return this.getInstance(TokenEndpointImpl.class,
                () -> new TokenEndpointImpl(this.issuerId(), this.codes(), this.users(), this.sessions(), this.keyset()));
    }

    public UserInfoEndpointImpl userInfoEndpoint() {
        return this.getInstance(UserInfoEndpointImpl.class,
                () -> new UserInfoEndpointImpl(this.claims(), this.accessTokenValidator()));
    }

    public AccessTokenValidator accessTokenValidator() {
        return this.getInstance(AccessTokenValidator.class,
                () -> new JWTAccessTokenValidator(
                        this.issuerId(),
                        this.keyset()));
    }

    public ConsentEndpoint consentEndpoint() {
        return this.getInstance(ConsentEndpointImpl.class,
                () -> new ConsentEndpointImpl(
                        this.ongoingAuths(),
                        this.users(),
                        this.users()
                ));
    }


    public AuthenticateEndpoint authenticateEndpoint() {
        return this.getInstance(AuthenticateEndpointImpl.class,
                () -> new AuthenticateEndpointImpl(
                        this.ongoingAuths(),
                        this.users(),
                        this.users(),
                        this.sessions(),
                        this.passwords()));
    }


    public AuthorizeEndpoint authorizeEndpoint() {
        return this.getInstance(AuthorizeEndpoint.class,
                () -> new AuthorizeEndpoint(
                        this.issuerId(), this.ongoingAuths(),
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
        if (this.fsBackEnd()) {
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
        if (this.fsBackEnd()) {
            return this.getInstance(
                    FSClaims.class,
                    () -> new FSClaims(this.userDataFileStorage()));
        } else {
            return this.getInstance(MemClaims.class,
                    MemClaims::new);
        }
    }

    private boolean fsBackEnd() {

        return this.args.arg("backend").orElse("fs").equals("fs");
    }

    private Path basePath() {
        return StartupArgs.storageFolder(this.args.arg("path").orElse(""));
    }

    public KeySet keyset() {
        return this.getInstance(MemKeySet.class, MemKeySet::new);
    }

    public FileStorage userDataFileStorage() {
        return this.getInstance(FileStorage.class, () -> {
            try {
                return new FileStorage(this.basePath());
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
