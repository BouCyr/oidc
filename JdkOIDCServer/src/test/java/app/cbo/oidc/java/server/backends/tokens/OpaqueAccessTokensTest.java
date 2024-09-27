package app.cbo.oidc.java.server.backends.tokens;

import app.cbo.oidc.java.server.backends.keys.MemKeySet;
import app.cbo.oidc.java.server.credentials.AuthenticationMode;
import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.http.userinfo.ForbiddenResponse;
import app.cbo.oidc.java.server.oidc.Issuer;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpaqueAccessTokensTest {

    public static final String USER_ID = "Cunégonde";
    public static final ClientId CLIENT_ID = ClientId.of("client_id_XYXY");
    public static final String RESOURCE = "API_agenda";
    public static final Issuer ISSUER = Issuer.of("meMyself");

    @Test
    void emptyInputValidation() throws ForbiddenResponse {
        var keyset = new MemKeySet();
        var tested = new OpaqueAccessTokens(ISSUER, keyset);

        assertThatThrownBy(() -> tested.validateAccessToken("")).isInstanceOf(ForbiddenResponse.class);

    }

    @Test
    void garbageInputValidation() throws ForbiddenResponse {
        var keyset = new MemKeySet();
        var tested = new OpaqueAccessTokens(ISSUER, keyset);

        assertThatThrownBy(() -> tested.validateAccessToken("dsf**/xc$$$")).isInstanceOf(ForbiddenResponse.class);

    }

    @Test
    void withoutScopes() throws ForbiddenResponse {
        var keyset = new MemKeySet();
        var tested = new OpaqueAccessTokens(ISSUER, keyset);

        var output = tested.generate(
                CLIENT_ID,
                new Session(UserId.of(USER_ID), EnumSet.of(AuthenticationMode.USER_FOUND, AuthenticationMode.TOTP_OK), emptySet()),
                RESOURCE);

        assertThat(output)
                .isNotNull().isNotBlank();

        var validated = tested.validateAccessToken(output);
        assertThat(validated)
                .isNotNull();

        assertThat(validated.sub()).isNotNull();
        assertThat(validated.sub().id()).isNotNull().isEqualTo(USER_ID);
        assertThat(validated.aud()).isNotBlank().isEqualTo(RESOURCE);
        assertThat(validated.scopes()).isNullOrEmpty();
    }

    @Test
    void withScopes() throws ForbiddenResponse {
        var keyset = new MemKeySet();
        var tested = new OpaqueAccessTokens(ISSUER, keyset);

        var output = tested.generate(
                CLIENT_ID,
                new Session(UserId.of(USER_ID), EnumSet.of(AuthenticationMode.USER_FOUND, AuthenticationMode.TOTP_OK), Set.of("aa", "bbb", "ccc")),
                RESOURCE);

        assertThat(output)
                .isNotNull().isNotBlank();

        var validated = tested.validateAccessToken(output);
        assertThat(validated)
                .isNotNull();

        assertThat(validated.sub()).isNotNull();
        assertThat(validated.sub().id()).isNotNull().isEqualTo(USER_ID);
        assertThat(validated.aud()).isNotBlank().isEqualTo(RESOURCE);
        assertThat(validated.scopes()).hasSize(3)
                .containsExactlyInAnyOrder("aa", "bbb", "ccc");
    }

}