package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.datastored.UserId;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class CodesTest {

    @Test
    void nominal(){
        var code = Codes.getInstance().createFor(()->"bob",()->"the_client_id");

        var userIdfoundBack = Codes.getInstance().consume(code, () ->"the_client_id");
        assertThat(userIdfoundBack)
                .isPresent()
                .get()
                    .extracting(UserId::getUserId)
                    .isEqualTo("bob");
    }

    @Test
    void code_consumed(){
        var code = Codes.getInstance().createFor(()->"bob",()->"the_client_id");

        var userIdfoundBack = Codes.getInstance().consume(code, () ->"the_client_id");
        assertThat(userIdfoundBack)
                .isPresent();

        var replay = Codes.getInstance().consume(code, () ->"the_client_id");
        assertThat(replay)
                .isEmpty();
    }

    @Test
    void wrong_code(){
        var code = Codes.getInstance().createFor(()->"bob",()->"the_client_id");

        var userIdfoundBack = Codes.getInstance().consume(()->"??", () ->"the_client_id");
        assertThat(userIdfoundBack)
                .isEmpty();
    }

    @Test
    void wrong_client(){
        var code = Codes.getInstance().createFor(()->"bob",()->"the_client_id");

        var userIdfoundBack = Codes.getInstance().consume(code, () ->"NYET");
        assertThat(userIdfoundBack)
                .isEmpty();
    }

    @Test
    void nullability_create(){
        assertThatThrownBy(() -> Codes.getInstance().createFor(null,null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Codes.getInstance().createFor(()->"bob",null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Codes.getInstance().createFor(null,()->"the_client_id"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullability_consume() {
        var code = Codes.getInstance().createFor(() -> "bob", () -> "the_client_id");

        assertThatThrownBy(() -> Codes.getInstance().consume(null,null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Codes.getInstance().consume(code,null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Codes.getInstance().createFor(null,()->"the_client_id"))
                .isInstanceOf(NullPointerException.class);
    }

}