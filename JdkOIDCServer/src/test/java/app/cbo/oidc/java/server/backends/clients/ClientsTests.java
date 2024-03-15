package app.cbo.oidc.java.server.backends.clients;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientsTests {

    static void test(ClientRegistry tested){
        assertThat(tested.getRegisteredClients()).isEmpty();


        tested.setClient("client1", "secret1");
        assertThat(tested.getRegisteredClients()).containsExactlyInAnyOrder("client1");

        assertThat(tested.authenticate("client1","secret1")).isTrue();
        assertThat(tested.authenticate("client1","secret2")).isFalse();
        assertThat(tested.authenticate(null,null)).isFalse();

        tested.setClient("client2", "secret2");
        assertThat(tested.getRegisteredClients()).containsExactlyInAnyOrder("client1","client2");
        assertThat(tested.authenticate("client1","secret1")).isTrue();
        assertThat(tested.authenticate("client1","secret2")).isFalse();
        assertThat(tested.authenticate(null,null)).isFalse();
        assertThat(tested.authenticate("client2","secret1")).isFalse();
        assertThat(tested.authenticate("client2","secret2")).isTrue();
        assertThat(tested.authenticate(null,null)).isFalse();
    }
}
