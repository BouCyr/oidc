package app.cbo.oidc.java.server.credentials;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class TOTPTest {


    @Test
    void nominal(){
        var one = TOTP.get("ALBACORE");
        var check = TOTP.confront(one, "ALBACORE");

        Assertions.assertThat(check)
                .isTrue();
    }

}