package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.backends.sessions.Sessions;
import app.cbo.oidc.java.server.credentials.AuthenticationMode;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.datastored.user.User;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SessionsTest {

    @Test
    void nominal() {

        var sessions = new Sessions();
        var session = sessions.createSession(new User("cyrille", null, null), EnumSet.of(AuthenticationMode.PASSWORD_OK));

        sessions.addAuthentications(session, EnumSet.of(AuthenticationMode.PASSWORD_OK, AuthenticationMode.TOTP_OK));

        var foundBack = sessions.find(session);
        assertThat(foundBack)
                .isPresent();

        assertThat(foundBack.get().userId().getUserId()).isEqualTo("cyrille");
        assertThat(foundBack.get().authentications()).containsExactly(AuthenticationMode.PASSWORD_OK, AuthenticationMode.TOTP_OK);

        assertThat(foundBack.get().authTime()).isCloseTo(LocalDateTime.now(), new TemporalUnitWithinOffset(5L, ChronoUnit.SECONDS));
    }

    @Test
    void nullability() {
        var sessions = new Sessions();
        var usr = new User("cyrille", null, null);
        assertThatThrownBy(() -> sessions.createSession(null, EnumSet.of(AuthenticationMode.TOTP_OK)))
                .isInstanceOf(NullPointerException.class);

        assertDoesNotThrow(() -> sessions.createSession(usr, EnumSet.noneOf(AuthenticationMode.class)));
        assertDoesNotThrow(() -> sessions.createSession(usr, null));

        assertThat(sessions.find(null)).isEmpty();
        assertThat(sessions.find(() -> null)).isEmpty();

        assertThatThrownBy(() -> sessions.addAuthentications(null, EnumSet.of(AuthenticationMode.TOTP_OK)))
                .isInstanceOf(NullPointerException.class);

        assertDoesNotThrow(() -> sessions.addAuthentications(SessionId.of(""), null));


    }

}