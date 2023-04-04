package app.cbo.oidc.java.server.utils;

import app.cbo.oidc.java.server.datastored.SessionId;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CookiesTest {

    @Test
    void findSessionCookie() {
        var cookiesFromHeader = Cookies.parseCookies(List.of("a=A","b=B","sessionId=XoXo"));
        var found = Cookies.findSessionCookie(cookiesFromHeader);

        Assertions.assertThat(found)
                .isPresent()
                .get().extracting(SessionId::getSessionId)
                .isEqualTo("XoXo");

    }

    @Test
    void noSessionCookie() {
        var cookiesFromHeader = Cookies.parseCookies(List.of("a=A","b=B"));
        var found = Cookies.findSessionCookie(cookiesFromHeader);

        Assertions.assertThat(found)
                .isEmpty();

    }

    @Test
    void parseCookies_empty() {
        Assertions.assertThat(Cookies.parseCookies(Collections.emptyList())).isEmpty();
        List<String> cookies =null;
        Assertions.assertThat(Cookies.parseCookies(cookies)).isEmpty();

    }

    @Test
    void parseCookies() {
        var cookiesFromHeader = Cookies.parseCookies(List.of("a=A","b=B","z"));

        Assertions.assertThat(cookiesFromHeader)
                .hasSize(3)
                .filteredOn(c -> "a".equals(c.name()))
                .hasSize(1)
                .element(0).matches(c -> "A".equals(c.value()));
        Assertions.assertThat(cookiesFromHeader)
                .filteredOn(c -> "b".equals(c.name()))
                .hasSize(1)
                .element(0).matches(c -> "B".equals(c.value()));
        Assertions.assertThat(cookiesFromHeader)
                .filteredOn(c -> "z".equals(c.name()))
                .hasSize(1)
                .element(0).matches(c -> c.value().isEmpty());
    }
}