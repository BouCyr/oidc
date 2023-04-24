package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.credentials.AuthenticationMode;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.datastored.User;
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
    void nominal(){

        var session = Sessions.getInstance().createSession(new User("cyrille",null,null), EnumSet.of(AuthenticationMode.PASSWORD_OK));

        Sessions.getInstance().addAuthentications(session, EnumSet.of(AuthenticationMode.PASSWORD_OK, AuthenticationMode.TOTP_OK));

        var foundback = Sessions.getInstance().find(session);
        assertThat(foundback)
                .isPresent();

        assertThat(foundback.get().userId().getUserId()).isEqualTo("cyrille");
        assertThat(foundback.get().authentications()).containsExactly(AuthenticationMode.PASSWORD_OK, AuthenticationMode.TOTP_OK);

        assertThat(foundback.get().authTime()).isCloseTo(LocalDateTime.now(), new TemporalUnitWithinOffset(5L, ChronoUnit.SECONDS));
    }

    @Test
    void nullability() {

        var usr = new User("cyrille", null, null);
        assertThatThrownBy(() -> Sessions.getInstance().createSession(null, EnumSet.of(AuthenticationMode.TOTP_OK)))
                .isInstanceOf(NullPointerException.class);

        assertDoesNotThrow(() -> Sessions.getInstance().createSession(usr, EnumSet.noneOf(AuthenticationMode.class)));
        assertDoesNotThrow(() -> Sessions.getInstance().createSession(usr, null));

        assertThat(Sessions.getInstance().find(null)).isEmpty();
        assertThat(Sessions.getInstance().find(() -> null)).isEmpty();

        assertThatThrownBy(() -> Sessions.getInstance().addAuthentications(null, EnumSet.of(AuthenticationMode.TOTP_OK)))
                .isInstanceOf(NullPointerException.class);

        assertDoesNotThrow(() -> Sessions.getInstance().addAuthentications(SessionId.of(""), null));


    }

}