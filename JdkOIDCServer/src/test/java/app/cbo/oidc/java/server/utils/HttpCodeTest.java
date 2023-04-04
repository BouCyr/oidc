package app.cbo.oidc.java.server.utils;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class HttpCodeTest {

    @Test
    void test(){
        Assertions.assertThat(HttpCode.OK.code()).isEqualTo(200);


    }

}