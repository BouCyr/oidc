package app.cbo.oidc.java.server.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryStringParserTest {

    @Test
    void test() {
        String qs = "tag=foo:bar&tag=fizz:buzz&lang=en";

        var result = QueryStringParser.from(qs);

        assertThat(result)
                .hasSize(2)
                .containsKey("tag")
                .containsKey("lang");

        assertThat(result.get("tag"))
                .hasSize(2);
        assertThat(result.get("lang"))
                .hasSize(1);
        assertThat(result.get("lang").iterator().next())
                .isEqualTo("en");
    }

    @Test
    void test_novalue() {
        String qs = "tagonly&tag=fizz:buzz&lang=en";
        var result = QueryStringParser.from(qs);

        assertThat(result)
                .hasSize(3)
                .containsKey("tagonly")
                .containsKey("tag")
                .containsKey("lang");

        assertThat(result.get("tag"))
                .hasSize(1);
        assertThat(result.get("lang"))
                .hasSize(1);
        assertThat(result.get("tagonly"))
                .hasSize(1);
        assertThat(result.get("tagonly").iterator().next())
                .isEmpty();
    }

    @Test
    void edge_case_empty() {
        String qs = "";
        var result = QueryStringParser.from(qs);
        assertThat(result).isEmpty();
    }

    @Test
    void edge_case_null() {
        String qs = null;
        var result = QueryStringParser.from(qs);
        assertThat(result).isEmpty();
    }

    @Test
    void edge_case1() {
        String qs = "one=&two=fizz";
        var result = QueryStringParser.from(qs);

        assertThat(result)
                .hasSize(2)
                .containsKey("one")
                .containsKey("two");

        assertThat(result.get("one"))
                .hasSize(1);
        assertThat(result.get("one").iterator().next())
              .isEqualTo("");
        assertThat(result.get("two").iterator().next())
      .isEqualTo("fizz");


    }

}