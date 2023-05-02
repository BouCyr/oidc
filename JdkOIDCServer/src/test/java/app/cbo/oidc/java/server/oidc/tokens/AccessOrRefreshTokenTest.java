package app.cbo.oidc.java.server.oidc.tokens;

import app.cbo.oidc.java.server.json.JSON;
import app.cbo.oidc.java.server.json.JsonProcessingException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;

class AccessOrRefreshTokenTest {

    @Test
    void nominal() {

        var tkn = new AccessOrRefreshToken(UUID.randomUUID().toString(), UUID.randomUUID().toString(), new Random().nextLong(), List.of(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        ));
        var json = JSON.jsonify(tkn);

        var back = AccessOrRefreshToken.fromJson(json);

        Assertions.assertThat(back.exp()).isEqualTo(tkn.exp());
        Assertions.assertThat(back.sub()).isEqualTo(tkn.sub());
        Assertions.assertThat(back.typ()).isEqualTo(tkn.typ());
        Assertions.assertThat(back.scopes()).containsExactlyInAnyOrderElementsOf(tkn.scopes());
    }

    @Test
    void emptyTyp() {
        var back = AccessOrRefreshToken.fromJson("{\n\"exp\": 5,\n\"sub\": \"sub\",\n\"scopes\": [\n\"a\",\n\"b\",\n\"c\"],\n\"typ\": \"\"\n}");
        Assertions.assertThat(back.exp()).isEqualTo(5L);
        Assertions.assertThat(back.sub()).isEqualTo("sub");
        Assertions.assertThat(back.typ()).isEqualTo("");
    }

    @Test
    void missingTyp() {
        Assertions.assertThatThrownBy(() -> AccessOrRefreshToken.fromJson("{\n\"exp\": 5,\n\"sub\": \"sub\",\n\"scopes\": [\n\"a\",\n\"b\",\n\"c\"]\n\n}"))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void emptyScopes() {
        var back = AccessOrRefreshToken.fromJson("{\n\"exp\": 5,\n\"sub\": \"sub\",\n\"scopes\": [],\n\"typ\": \"typ\"\n}");
        Assertions.assertThat(back.scopes()).isEmpty();
    }

    @Test
    void stringExp() {
        Assertions.assertThatThrownBy(() -> AccessOrRefreshToken.fromJson("{\n\"exp\": \"5\",\n\"sub\": \"sub\",\n\"scopes\": [],\n\"typ\": \"typ\"\n}"))
                .isInstanceOf(JsonProcessingException.class);
    }

    @Test
    void unordered() {

        check("{\n\"exp\": 5,\n\"sub\": \"sub\",\n\"scopes\": [\n\"a\",\n\"b\",\n\"c\"],\n\"typ\": \"typ\"\n}");
        check("{\"scopes\": [\n\"a\",\n\"b\",\n\"c\"],\n\"typ\": \"typ\"\n}\n\n\r\n,\n\"exp\": 5,\n\"sub\": \"sub\",\n");
    }

    private void check(String json) {
        var back = AccessOrRefreshToken.fromJson(json);

        Assertions.assertThat(back.exp()).isEqualTo(5L);
        Assertions.assertThat(back.sub()).isEqualTo("sub");
        Assertions.assertThat(back.typ()).isEqualTo("typ");
        Assertions.assertThat(back.scopes()).containsExactlyInAnyOrder("a", "c", "b");
    }
}