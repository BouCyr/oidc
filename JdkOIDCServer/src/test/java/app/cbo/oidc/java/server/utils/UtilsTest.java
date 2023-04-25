package app.cbo.oidc.java.server.utils;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UtilsTest {

    @Test
    void isEmpty() {
        assertThat(Utils.isEmpty("")).isTrue();
        assertThat(Utils.isEmpty("   ")).isFalse();
        assertThat(Utils.isEmpty("  ff  ")).isFalse();
        String nullS = null;
        assertThat(Utils.isEmpty(nullS)).isTrue();
    }

    @Test
    void isBlank() {
        assertThat(Utils.isBlank("")).isTrue();
        assertThat(Utils.isBlank("   ")).isTrue();
        assertThat(Utils.isBlank("  ff  ")).isFalse();
        String nullS = null;
        assertThat(Utils.isBlank(nullS)).isTrue();
    }

    @Test
    void testIsEmpty() {
        assertThat(Utils.isEmpty(Optional.of(""))).isTrue();
        assertThat(Utils.isEmpty(Optional.of("   "))).isFalse();
        assertThat(Utils.isEmpty(Optional.of("  ff  "))).isFalse();
        Optional<String> nullS = null;
        assertThat(Utils.isEmpty(nullS)).isTrue();
        assertThat(Utils.isEmpty(Optional.empty())).isTrue();
    }

    @Test
    void testIsBlank() {
        assertThat(Utils.isBlank(Optional.of(""))).isTrue();
        assertThat(Utils.isBlank(Optional.of("   "))).isTrue();
        assertThat(Utils.isBlank(Optional.of("  ff  "))).isFalse();
        Optional<String> nullS = null;
        assertThat(Utils.isBlank(nullS)).isTrue();
        assertThat(Utils.isBlank(Optional.empty())).isTrue();
    }

    @Test
    void testIsEmpty_coll() {

        Collection<String> nullC = null;
        assertThat(Utils.isEmpty(nullC)).isTrue();
        assertThat(Utils.isEmpty(Collections.emptyList())).isTrue();
        assertThat(Utils.isEmpty(List.of(""))).isFalse();
        assertThat(Utils.isEmpty(List.of("",""))).isFalse();
        assertThat(Utils.isEmpty(List.of("","",""))).isFalse();
        assertThat(Utils.isEmpty(List.of("","a",""))).isFalse();
        assertThat(Utils.isEmpty(List.of(""," ",""))).isFalse();

    }

    @Test
    void testIsBlank_coll() {
        Collection<String> nullC = null;
        assertThat(Utils.isBlank(nullC)).isTrue();
        assertThat(Utils.isBlank(Collections.emptyList())).isTrue();
        assertThat(Utils.isBlank(List.of(""))).isTrue();
        assertThat(Utils.isBlank(List.of("",""))).isTrue();
        assertThat(Utils.isBlank(List.of("","",""))).isTrue();
        assertThat(Utils.isBlank(List.of("","a",""))).isFalse();
        assertThat(Utils.isBlank(List.of(""," ",""))).isTrue();
    }
}