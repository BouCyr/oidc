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

        Assertions.assertThatThrownBy(() ->  PasswordEncoder.getInstance().confront(null, null))
                .isInstanceOf(NullPointerException.class);
        Assertions.assertThatThrownBy(() ->  PasswordEncoder.getInstance().confront("a", null))
                .isInstanceOf(NullPointerException.class);
        Assertions.assertThatThrownBy(() ->  PasswordEncoder.getInstance().confront(null, "b"))
                .isInstanceOf(NullPointerException.class);
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