package app.cbo.oidc.java.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class StartupArgsTest {


    @Test
    void test_nominal() {

        var args = StartupArgs.from("port=8052");
        Assertions.assertEquals(args.port(), 8052);
    }

    @Test
    void test_nominal_2() {

        var args = StartupArgs.from("test=foo", "port=8053", "eg=bar");
        Assertions.assertEquals(args.port(), 8053);
    }

    @Test
    void test_nominal_3() {

        var args = StartupArgs.from("test=foo", "eg=bar");
        Assertions.assertEquals(args.port(), 9451);
    }

    @Test
    void test_invalid() {
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> StartupArgs.from("port="));

    }

    @Test
    void test_invalid_2() {
        Assertions.assertThrows(NumberFormatException.class, () -> StartupArgs.from("port=NOTANTINGER"));
    }

}