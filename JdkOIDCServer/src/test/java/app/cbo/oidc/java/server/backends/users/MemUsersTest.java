package app.cbo.oidc.java.server.backends.users;

import org.junit.jupiter.api.Test;

public class MemUsersTest {

    @Test
    void readWrite() {
        UsersTest.testReadWrite(new MemUsers( UsersTest.passwords()));
    }
}
