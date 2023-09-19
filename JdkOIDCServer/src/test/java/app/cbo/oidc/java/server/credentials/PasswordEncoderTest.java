package app.cbo.oidc.java.server.credentials;

import app.cbo.oidc.java.server.credentials.pwds.PBKDF2WithHmacSHA1PasswordHash;
import app.cbo.oidc.java.server.credentials.pwds.Passwords;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class PasswordEncoderTest {


    private final Passwords tested = new PBKDF2WithHmacSHA1PasswordHash();

    @Test
    void nominal(){


        String password = "hunter2";

        var encoded = tested.encode(password);
        var check = tested.confront("hunter2", encoded);

        Assertions.assertThat(check)
                .isTrue();
    }

    @Test
    void nominal_blank(){
        String password = "";

        var encoded = tested.encode(password);
        var check = tested.confront("", encoded);

        Assertions.assertThat(check)
                .isTrue();
    }

    @Test
    void diff(){
        String password = "hunter2";

        var encoded = tested.encode(password);
        var check = tested.confront("sesame", encoded);

        Assertions.assertThat(check)
                .isFalse();
    }

    @Test
    void nullability(){

        Assertions.assertThatThrownBy(() ->  tested.encode(null))
                .isInstanceOf(NullPointerException.class);

        Assertions.assertThat(tested.confront(null, null))
                .isFalse();
        Assertions.assertThat(tested.confront("a", null))
                .isFalse();
        Assertions.assertThat(tested.confront(null, "b"))
                .isFalse();
    }

    @Test
    void blankProvided_nnstored(){
        String password = "";

        var encoded = tested.encode(password);
        var check = tested.confront("sesame", encoded);

        Assertions.assertThat(check)
                .isFalse();
    }


}