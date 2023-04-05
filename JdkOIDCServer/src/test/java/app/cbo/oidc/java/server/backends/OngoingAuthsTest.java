package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.datastored.UserId;
import app.cbo.oidc.java.server.endpoints.authorize.AuthorizeEndpointParams;
import app.cbo.oidc.java.server.oidc.OIDCDisplayValues;
import app.cbo.oidc.java.server.oidc.OIDCPromptValues;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

class OngoingAuthsTest {

    @Test
    void nominal(){

        AuthorizeEndpointParams p = createParams();
        var code = OngoingAuths.getInstance().store(p);

        var foundBack = OngoingAuths.getInstance().retrieve(code);

        assertThat(foundBack).isPresent();
        var got = foundBack.get();
        assertThat(got.maxAge()).isPresent()
                .get().isEqualTo("maxAge");


    }

    @Test
    void code_consumed(){
        var code = OngoingAuths.getInstance().store(this.createParams());

        var foundBack = OngoingAuths.getInstance().retrieve(code);
        assertThat(foundBack)
                .isPresent();

        var replay = OngoingAuths.getInstance().retrieve(code );
        assertThat(replay)
                .isEmpty();
    }

    @Test
    void wrong_code(){
        var code = OngoingAuths.getInstance().store(this.createParams());

        var foundBack = OngoingAuths.getInstance().retrieve(()->"??");
        assertThat(foundBack)
                .isEmpty();
    }



    @Test
    void nullability_create(){
        assertThatThrownBy(() -> OngoingAuths.getInstance().store(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void nullability_consume() {

        assertThatThrownBy(() -> OngoingAuths.getInstance().retrieve(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> OngoingAuths.getInstance().retrieve(() -> null))
                .isInstanceOf(NullPointerException.class);

    }


    private AuthorizeEndpointParams createParams() {
        AuthorizeEndpointParams p = new AuthorizeEndpointParams(

                List.of("openid"),   //List<String> scope,
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
        return p;
    }
}