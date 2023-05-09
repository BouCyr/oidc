package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.backends.ongoingAuths.OngoingAuths;
import app.cbo.oidc.java.server.datastored.OngoingAuthId;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeParams;
import app.cbo.oidc.java.server.oidc.OIDCDisplayValues;
import app.cbo.oidc.java.server.oidc.OIDCPromptValues;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OngoingAuthsTest {

    @Test
    void nominal() {


        var auths = new OngoingAuths();
        AuthorizeParams p = createParams();
        var code = auths.store(p);

        var foundBack = auths.find(code);

        assertThat(foundBack).isPresent();
        var got = foundBack.get();
        assertThat(got.maxAge()).isPresent()
                .get().isEqualTo("maxAge");


    }

    @Test
    void code_consumed() {
        var auths = new OngoingAuths();
        var code = auths.store(this.createParams());

        var foundBack = auths.find(code);
        assertThat(foundBack)
                .isPresent();

        var replay = auths.find(code);
        assertThat(replay)
                .isEmpty();
    }

    @Test
    void wrong_code() {
        var auths = new OngoingAuths();
        var code = auths.store(this.createParams());

        var foundBack = auths.find(() -> "??");
        assertThat(foundBack)
                .isEmpty();
    }




    @Test
    void nullability_consume() {
        var auths = new OngoingAuths();
        assertThatThrownBy(() -> auths.find(null))
                .isInstanceOf(NullPointerException.class);
        assertThat(auths.find(OngoingAuthId.of(null)))
                .isEmpty();

    }


    private AuthorizeParams createParams() {
        return new AuthorizeParams(

                List.of("openid"),   //List<String> scopes,
                List.of("rs"),  //List<String> responseTypes,
                Optional.of("clientId"),  //Optional<String> clientId,
                Optional.of("redirectUri"),  //Optional<String> redirectUri,
                Optional.of("state"),//Optional<String> state,
                Optional.of("responseMode"),//Optional<String> responseMode,
                Optional.of("nonce"),//Optional<String> nonce,
                Optional.of(OIDCDisplayValues.PAGE),//Optional<OIDCDisplayValues> display,
                List.of(OIDCPromptValues.LOGIN),//List<OIDCPromptValues> prompt,
                Optional.of("maxAge"),//Optional<String> maxAge,
                List.of("uiLocales"),//List<String> uiLocales,
                Optional.of("idTokenHint"),//Optional<String> idTokenHint,
                Optional.of("loginHint"),//Optional<String> loginHint,
                List.of("acrValues")//List<String> acrValues
        );
    }
}