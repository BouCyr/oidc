package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.credentials.AuthenticationMode;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.datastored.User;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.TemporalOffset;
import org.assertj.core.data.TemporalUnitWithinOffset;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class SessionsTest {

    @Test
    void nominal(){

        var session = Sessions.getInstance().createSession(new User("cyrille",null,null), EnumSet.of(AuthenticationMode.PASSWORD_OK));

        Sessions.getInstance().addAuthentications(session, EnumSet.of(AuthenticationMode.PASSWORD_OK, AuthenticationMode.TOTP_OK));

        var foundback = Sessions.getInstance().getSession(session);
        assertThat(foundback)
                .isPresent();

        assertThat(foundback.get().userId().getUserId()).isEqualTo("cyrille");
        assertThat(foundback.get().authentications()).containsExactly(AuthenticationMode.PASSWORD_OK, AuthenticationMode.TOTP_OK);

        assertThat(foundback.get().authTime()).isCloseTo(LocalDateTime.now(), new TemporalUnitWithinOffset(5L, ChronoUnit.SECONDS));
    }

    @Test
    void nullability(){

        var usr = new User("cyrille",null,null);
        assertThatThrownBy( () -> Sessions.getInstance().createSession(null, EnumSet.of(AuthenticationMode.TOTP_OK)))
                .isInstanceOf(NullPointerException.class);

        assertDoesNotThrow(() -> Sessions.getInstance().createSession(usr, EnumSet.noneOf(AuthenticationMode.class)));
        assertDoesNotThrow(() -> Sessions.getInstance().createSession(usr, null));

        assertThat(Sessions.getInstance().getSession(null)).isEmpty();
        assertThat(Sessions.getInstance().getSession(()->null)).isEmpty();

        assertThatThrownBy( () -> Sessions.getInstance().addAuthentications(null, EnumSet.of(AuthenticationMode.TOTP_OK)))
                .isInstanceOf(NullPointerException.class);

        assertDoesNotThrow(() -> Sessions.getInstance().addAuthentications(SessionId.of(""), null));


    }

}