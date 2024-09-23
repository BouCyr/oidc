package app.cbo.oidc.java.server.backends.clients;

import app.cbo.oidc.java.server.datastored.ClientId;

import static org.assertj.core.api.Assertions.assertThat;

public class ClientsTests {

    static void test(ClientRegistry tested){
        assertThat(tested.getRegisteredClients()).isEmpty();


        tested.setClient(ClientId.of("client1"), "secret1");
        assertThat(tested.getRegisteredClients()).containsExactlyInAnyOrder(ClientId.of("client1"));

        assertThat(tested.authenticate(ClientId.of("client1"),"secret1")).isTrue();
        assertThat(tested.authenticate(ClientId.of("client1"),"secret2")).isFalse();
        assertThat(tested.authenticate(null,null)).isFalse();

        tested.setClient(ClientId.of("client2"), "secret2");
        assertThat(tested.getRegisteredClients()).containsExactlyInAnyOrder(ClientId.of("client1"),ClientId.of("client2"));
        assertThat(tested.authenticate(ClientId.of("client1"),"secret1")).isTrue();
        assertThat(tested.authenticate(ClientId.of("client1"),"secret2")).isFalse();
        assertThat(tested.authenticate(null,null)).isFalse();
        assertThat(tested.authenticate(ClientId.of("client2"),"secret1")).isFalse();
        assertThat(tested.authenticate(ClientId.of("client2"),"secret2")).isTrue();
        assertThat(tested.authenticate(null,null)).isFalse();
    }
}
