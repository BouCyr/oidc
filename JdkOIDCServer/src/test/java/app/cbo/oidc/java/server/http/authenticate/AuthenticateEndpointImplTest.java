package app.cbo.oidc.java.server.http.authenticate;

import app.cbo.oidc.java.server.backends.users.MemUsers;
import app.cbo.oidc.java.server.credentials.AuthenticationMode;
import app.cbo.oidc.java.server.credentials.TOTP;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.datastored.user.User;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.http.AuthErrorInteraction;
import app.cbo.oidc.java.server.http.authorize.AuthorizeParams;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticateEndpointImplTest {

    @Test
    void no_session() throws AuthErrorInteraction {
        AuthenticateEndpointImpl tested = new AuthenticateEndpointImpl(
                key -> Optional.empty(),
                userId -> Optional.empty(),
                (x, y, z) -> UserId.of(x),
                (user, authenticationModes) -> new SessionId.Simple("sessionId"),
                (provided, storedEncoded) -> true
        );

        var interaction = tested.treatRequest(Collections.emptyMap());
        assertThat(interaction)
                .isInstanceOf(DisplayLoginFormInteraction.class);
    }

    @Test
    void unknown_login_provided() throws AuthErrorInteraction {

        final EnumSet<AuthenticationMode> modes = EnumSet.noneOf(AuthenticationMode.class);
        final AtomicReference<User> loggedIn = new AtomicReference<>();


        AuthenticateEndpointImpl tested = createAuthEndpoint(loggedIn, modes);

        tested.treatRequest(Map.of("login", List.of("bob")));

        assertThat(loggedIn)
                .hasValueMatching(u -> u.getUserId().equals(UserId.of("bob")));
        assertThat(modes)
                .isNotEmpty()
                .containsExactly(AuthenticationMode.DECLARATIVE);


    }

    private static AuthenticateEndpointImpl createAuthEndpoint(AtomicReference<User> loggedIn, EnumSet<AuthenticationMode> modes) {
        var memUSers = new MemUsers(p -> p);
        AuthenticateEndpointImpl tested = new AuthenticateEndpointImpl(
                key -> Optional.of(new AuthorizeParams(Collections.emptyMap())),
                //user finder
                memUSers,
                //user creator
                memUSers,
                (user, authenticationModes) -> {
                    loggedIn.set(user);
                    modes.addAll(authenticationModes);
                    return new SessionId.Simple("sessionId");
                },
                (provided, storedEncoded) -> true
        );
        return tested;
    }

    @Test
    void known_login_provided() throws AuthErrorInteraction {

        final EnumSet<AuthenticationMode> modes = EnumSet.noneOf(AuthenticationMode.class);
        final AtomicReference<User> loggedIn = new AtomicReference<>();
        AuthenticateEndpointImpl tested = new AuthenticateEndpointImpl(
                key -> Optional.of(new AuthorizeParams(Collections.emptyMap())),
                userId -> Optional.of(new User("bob", "pwd", "topt")),
                (x, y, z) -> UserId.of(x),
                (user, authenticationModes) -> {
                    loggedIn.set(user);
                    modes.addAll(authenticationModes);
                    return new SessionId.Simple("sessionId");
                },
                (provided, storedEncoded) -> true
        );

        var interaction = tested.treatRequest(Map.of("login", List.of("bob")));
        assertThat(interaction)
                .isInstanceOf(AuthenticationSuccessfulInteraction.class);

        assertThat(loggedIn)
                .hasValueMatching(u -> u.getUserId().equals(UserId.of("bob")));
        assertThat(modes)
                .isNotEmpty()
                .containsExactly(AuthenticationMode.DECLARATIVE, AuthenticationMode.USER_FOUND);

    }

    @Test
    void login_pwd_provided() throws AuthErrorInteraction {

        final EnumSet<AuthenticationMode> modes = EnumSet.noneOf(AuthenticationMode.class);
        final AtomicReference<User> loggedIn = new AtomicReference<>();
        AuthenticateEndpointImpl tested = new AuthenticateEndpointImpl(
                key -> Optional.of(new AuthorizeParams(Collections.emptyMap())),
                userId -> Optional.of(new User("bob", "pwd", "topt")),
                (x, y, z) -> UserId.of(x),
                (user, authenticationModes) -> {
                    loggedIn.set(user);
                    modes.addAll(authenticationModes);
                    return new SessionId.Simple("sessionId");
                },
                (provided, storedEncoded) -> true
        );

        var interaction = tested.treatRequest(Map.of(
                "login", List.of("bob"),
                "pwd", List.of("pwd")));
        assertThat(interaction)
                .isInstanceOf(AuthenticationSuccessfulInteraction.class);

        assertThat(loggedIn)
                .hasValueMatching(u -> u.getUserId().equals(UserId.of("bob")));
        assertThat(modes)
                .isNotEmpty()
                .containsExactly(AuthenticationMode.DECLARATIVE,
                        AuthenticationMode.USER_FOUND,
                        AuthenticationMode.PASSWORD_OK);

    }

    @Test
    void wrongPwd_provided() {

        final EnumSet<AuthenticationMode> modes = EnumSet.noneOf(AuthenticationMode.class);
        final AtomicReference<User> loggedIn = new AtomicReference<>();
        AuthenticateEndpointImpl tested = new AuthenticateEndpointImpl(
                key -> Optional.of(new AuthorizeParams(Collections.emptyMap())),
                userId -> Optional.of(new User("bob", "pwd", "topt")),
                (x, y, z) -> UserId.of(x),
                (user, authenticationModes) -> {
                    loggedIn.set(user);
                    modes.addAll(authenticationModes);
                    return new SessionId.Simple("sessionId");
                },
                (provided, storedEncoded) -> false //FALSE !!!
        );

        assertThatThrownBy(() -> tested.treatRequest(
                Map.of(
                        "login", List.of("bob"),
                        "pwd", List.of("pwd"))))
                .isInstanceOf(AuthErrorInteraction.class);


    }

    @Test
    void wrongTotp_provided() {

        final EnumSet<AuthenticationMode> modes = EnumSet.noneOf(AuthenticationMode.class);
        final AtomicReference<User> loggedIn = new AtomicReference<>();
        AuthenticateEndpointImpl tested = new AuthenticateEndpointImpl(
                key -> Optional.of(new AuthorizeParams(Collections.emptyMap())),
                userId -> Optional.of(new User("bob", "pwd", "ALBACORE")),
                (x, y, z) -> UserId.of(x),
                (user, authenticationModes) -> {
                    loggedIn.set(user);
                    modes.addAll(authenticationModes);
                    return new SessionId.Simple("sessionId");
                },
                (provided, storedEncoded) -> true
        );

        assertThatThrownBy(() -> tested.treatRequest(
                Map.of(
                        "login", List.of("bob"),
                        "pwd", List.of("pwd"),
                        "totp", List.of("1324567")))) //WRONG TOTP
                .isInstanceOf(AuthErrorInteraction.class);


    }

    @Test
    void login_pwd_totp_provided() throws AuthErrorInteraction {

        final EnumSet<AuthenticationMode> modes = EnumSet.noneOf(AuthenticationMode.class);
        final AtomicReference<User> loggedIn = new AtomicReference<>();
        AuthenticateEndpointImpl tested = new AuthenticateEndpointImpl(
                key -> Optional.of(new AuthorizeParams(Collections.emptyMap())),
                userId -> Optional.of(new User("bob", "pwd", "ALBACORE")),
                (x, y, z) -> UserId.of(x),
                (user, authenticationModes) -> {
                    loggedIn.set(user);
                    modes.addAll(authenticationModes);
                    return new SessionId.Simple("sessionId");
                },
                (provided, storedEncoded) -> true
        );


        var interaction = tested.treatRequest(Map.of(
                "login", List.of("bob"),
                "pwd", List.of("pwd"),
                "totp", List.of(TOTP.get("ALBACORE"))));
        assertThat(interaction)
                .isInstanceOf(AuthenticationSuccessfulInteraction.class);

        assertThat(loggedIn)
                .hasValueMatching(u -> u.getUserId().equals(UserId.of("bob")));
        assertThat(modes)
                .isNotEmpty()
                .containsExactly(AuthenticationMode.DECLARATIVE,
                        AuthenticationMode.USER_FOUND,
                        AuthenticationMode.PASSWORD_OK,
                        AuthenticationMode.TOTP_OK);

    }

}