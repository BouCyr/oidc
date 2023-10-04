package app.cbo.oidc.java.server.http.authenticate;

import app.cbo.oidc.java.server.credentials.AuthenticationMode;
import app.cbo.oidc.java.server.credentials.TOTP;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.datastored.user.User;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.http.AuthErrorInteraction;
import app.cbo.oidc.java.server.http.authorize.AuthorizeParams;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticateEndpointImplTest {

    @Test
    void no_session() throws AuthErrorInteraction {
        AuthenticateEndpointImpl tested = new AuthenticateEndpointImpl(
                key -> Optional.empty(),
                userId -> Optional.empty(),
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
        AuthenticateEndpointImpl tested = new AuthenticateEndpointImpl(
                key -> Optional.of(new AuthorizeParams(Collections.emptyMap())),
                userId -> Optional.empty(), // NO USER !!!

                (user, authenticationModes) -> {
                    loggedIn.set(user);
                    modes.addAll(authenticationModes);
                    return new SessionId.Simple("sessionId");
                },
                (provided, storedEncoded) -> true
        );

        assertThatThrownBy(() -> tested.treatRequest(Map.of("login", List.of("bob"))))
                .isInstanceOf(AuthErrorInteraction.class);

        /* TODO [03/10/2023] declarative login
        Assertions.assertThat(loggedIn)
                .hasValueMatching(u -> u.getUserId().equals(UserId.of("bob")));
        Assertions.assertThat(modes)
                .isNotEmpty()
                .hasSize(2)
                .containsExactly(AuthenticationMode.DECLARATIVE);

         */

    }

    @Test
    void known_login_provided() throws AuthErrorInteraction {

        final EnumSet<AuthenticationMode> modes = EnumSet.noneOf(AuthenticationMode.class);
        final AtomicReference<User> loggedIn = new AtomicReference<>();
        AuthenticateEndpointImpl tested = new AuthenticateEndpointImpl(
                key -> Optional.of(new AuthorizeParams(Collections.emptyMap())),
                userId -> Optional.of(new User("bob", "pwd", "topt")),
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