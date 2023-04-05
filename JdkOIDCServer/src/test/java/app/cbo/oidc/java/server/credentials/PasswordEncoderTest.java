package app.cbo.oidc.java.server.credentials;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordEncoderTest {

    @Test
    void nominal(){
        String password = "hunter2";

        var encoded = PasswordEncoder.getInstance().encodePassword(password);
        var check = PasswordEncoder.getInstance().confront("hunter2", encoded);

        Assertions.assertThat(check)
                .isTrue();
    }

    @Test
    void nominal_blank(){
        String password = "";

        var encoded = PasswordEncoder.getInstance().encodePassword(password);
        var check = PasswordEncoder.getInstance().confront("", encoded);

        Assertions.assertThat(check)
                .isTrue();
    }

    @Test
    void diff(){
        String password = "hunter2";

        var encoded = PasswordEncoder.getInstance().encodePassword(password);
        var check = PasswordEncoder.getInstance().confront("sesame", encoded);

        Assertions.assertThat(check)
                .isFalse();
    }

    @Test
    void nullability(){

        Assertions.assertThatThrownBy(() ->  PasswordEncoder.getInstance().encodePassword(null))
                .isInstanceOf(NullPointerException.class);

        Assertions.assertThat(PasswordEncoder.getInstance().confront(null, null))
                .isFalse();
        Assertions.assertThat(PasswordEncoder.getInstance().confront("a", null))
                .isFalse();
        Assertions.assertThat(PasswordEncoder.getInstance().confront(null, "b"))
                .isFalse();
    }

    @Test
    void blankProvided_nnstored(){
        String password = "";

        var encoded = PasswordEncoder.getInstance().encodePassword(password);
        var check = PasswordEncoder.getInstance().confront("sesame", encoded);

        Assertions.assertThat(check)
                .isFalse();
    }


}