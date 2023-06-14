package app.cbo.oidc.java.server.backends.keys;

import org.junit.jupiter.api.Test;

public class Mem extends KeySetTest {

    @Test
    void mem() {
        this.nominal(new MemKeySet());
    }
}
