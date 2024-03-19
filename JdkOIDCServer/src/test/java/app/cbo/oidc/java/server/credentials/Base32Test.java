package app.cbo.oidc.java.server.credentials;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Base64;

class Base32Test {

    @Test
    void encode() {

        byte[] data = new SecretGenerator().generateSecret();
        String mine = Base32.encode(data);
        String theirs = new org.apache.commons.codec.binary.Base32().encodeAsString(data);

        Assertions.assertThat(mine)
                .isEqualTo(theirs);
    }

    @Test
    void decode() {

        byte[] data = new SecretGenerator().generateSecret();
        String asb64 = Base64.getEncoder().encodeToString(data);


        String theirs = new org.apache.commons.codec.binary.Base32().encodeAsString(data);
        byte[] back = Base32.decode(theirs);
        String asB64Back = Base64.getEncoder().encodeToString(back);


        Assertions.assertThat(asB64Back)
                .isEqualTo(asb64);
    }
}