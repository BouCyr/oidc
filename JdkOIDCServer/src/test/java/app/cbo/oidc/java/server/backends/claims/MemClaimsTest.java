package app.cbo.oidc.java.server.backends.claims;

import org.junit.jupiter.api.Test;

public class MemClaimsTest {
    @Test
    void testReadWrite() {

        ClaimsTest.testReadWrite(new MemClaims());
    }
}
