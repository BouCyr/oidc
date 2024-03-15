package app.cbo.oidc.java.server.backends.clients;

import org.junit.jupiter.api.Test;

public class MemClientRegistryTests {

    @Test
    void test() {
        //GIVEN//WHEN/THEN,etc.
        ClientsTests.test(new MemClientRegistry());
    }
}
