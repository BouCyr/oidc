package app.cbo.oidc.java.server.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QueryStringBuilderTest {

    @Test
    void test(){

        var tested = new QueryStringBuilder()
                .add("one")
                .add("two=deux")
                .add("three=trois")
                .add("four")
                .toString();

        Assertions.assertThat(tested)
                .isEqualTo("one&two=deux&three=trois&four");



    }

}