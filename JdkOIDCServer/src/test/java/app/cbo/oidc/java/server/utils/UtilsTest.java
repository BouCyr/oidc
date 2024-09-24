package app.cbo.oidc.java.server.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class UtilsTest {

    public static Stream<Arguments> provideStringsForIsBlank() {
        return Stream.of(
                Arguments.of("", true, true),
                Arguments.of("  ", true, false),
                Arguments.of("     \t\t ", true, false),
                Arguments.of("\t", true, false),
                Arguments.of("\r", true, false),
                Arguments.of("\n", true, false),
                Arguments.of("\r\n", true, false),
                Arguments.of("\n\r", true, false),
                Arguments.of("XX", false, false),
                Arguments.of("  XX", false, false),
                Arguments.of(" \rXX", false, false),
                Arguments.of(" \nXX", false, false),
                Arguments.of(" \r\nXX", false, false),
                Arguments.of(null, true, true)
        );
    }

    @ParameterizedTest
    @MethodSource("provideStringsForIsBlank")
    void isEmpty(String input, boolean blank, boolean empty) {
        assertThat(Utils.isEmpty(input)).isEqualTo(empty);
        assertThat(Utils.isEmpty(() -> input)).isEqualTo(empty);
    }

    @ParameterizedTest
    @MethodSource("provideStringsForIsBlank")
    void isBlank(String input, boolean blank, boolean empty) {
        assertThat(Utils.isBlank(input)).isEqualTo(blank);
        assertThat(Utils.isBlank(() -> input)).isEqualTo(blank);
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