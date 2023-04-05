package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Code;
import app.cbo.oidc.java.server.datastored.UserId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodesTest {

    @Test
    void nominal(){
        var code = Codes.getInstance().createFor(UserId.of("bob"), ClientId.of("the_client_id"));

        var userIdfoundBack = Codes.getInstance().consume(code, ClientId.of("the_client_id"));
        assertThat(userIdfoundBack)
                .isPresent()
                .get()
                    .extracting(UserId::getUserId)
                    .isEqualTo("bob");
    }

    @Test
    void code_consumed(){
        var code = Codes.getInstance().createFor(UserId.of("bob"),ClientId.of("the_client_id"));

        var userIdfoundBack = Codes.getInstance().consume(code, ClientId.of("the_client_id"));
        assertThat(userIdfoundBack)
                .isPresent();

        var replay = Codes.getInstance().consume(code, ClientId.of("the_client_id"));
        assertThat(replay)
                .isEmpty();
    }

    @Test
    void wrong_code(){
        var code = Codes.getInstance().createFor(UserId.of("bob"), ClientId.of("the_client_id"));

        var userIdfoundBack = Codes.getInstance().consume(Code.of("??"), ClientId.of("the_client_id"));
        assertThat(userIdfoundBack)
                .isEmpty();
    }

    @Test
    void wrong_client(){
        var code = Codes.getInstance().createFor(UserId.of("bob"), ClientId.of("the_client_id"));

        var userIdfoundBack = Codes.getInstance().consume(code, ClientId.of("ANOTHER_client_id"));
        assertThat(userIdfoundBack)
                .isEmpty();
    }

    @Test
    void nullability_create(){
        assertThatThrownBy(() -> Codes.getInstance().createFor(null,null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Codes.getInstance().createFor(UserId.of("bob"),null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Codes.getInstance().createFor(null, ClientId.of("the_client_id")))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullability_consume() {
        var code = Codes.getInstance().createFor(UserId.of("bob"),  ClientId.of("the_client_id"));

        assertThatThrownBy(() -> Codes.getInstance().consume(null,null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Codes.getInstance().consume(code,null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Codes.getInstance().createFor(null,ClientId.of("the_client_id")))
                .isInstanceOf(NullPointerException.class);
    }

}