package app.cbo.oidc.java.server.backends.codes;

import org.junit.jupiter.api.Test;

class MemCodesTest extends CodesTest {


    @Test
    void nominal() {

        nominal(new MemCodes());
    }

    @Test
    void code_consumed() {
        code_consumed(new MemCodes());
    }

    @Test
    void wrong_code() {

        wrongCode(new MemCodes());
    }

    @Test
    void wrong_client() {

        wrong_client(new MemCodes());
    }

    @Test
    void wrong_redirectUri() {

        wrong_redirecturi(new MemCodes());
    }
    @Test
    void nullability_create() {

        nullability(new MemCodes());
    }

    @Test
    void nullability_consume() {

        nullability_consume(new MemCodes());
    }


}