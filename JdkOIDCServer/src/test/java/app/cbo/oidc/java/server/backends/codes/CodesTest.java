package app.cbo.oidc.java.server.backends.codes;

import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.datastored.Code;
import app.cbo.oidc.java.server.datastored.SessionId;
import app.cbo.oidc.java.server.datastored.user.UserId;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CodesTest {


    public static final String REDIRECT_URI = "http://www.example.com";
    public static final String THE_CLIENT_ID = "the_client_id";
    public static final String THE_SESSION_ID = "the_client_id";
    public static final String BOB = "bob";
    public static final List<String> SCOPES = List.of("openid", "profile", "email");

    protected void nominal(Codes codes) {
        var code = codes.createFor(UserId.of(BOB),
                ClientId.of(THE_CLIENT_ID),
                SessionId.of(THE_SESSION_ID),
                REDIRECT_URI,
                SCOPES,
                UUID.randomUUID().toString());

        var userIdFoundBack = codes.consume(
                code, ClientId.of(THE_CLIENT_ID), REDIRECT_URI);
        assertThat(userIdFoundBack)
                .isPresent()
                .get()
                .extracting(codeData -> codeData.userId().getUserId())
                .isEqualTo(BOB);
    }


    protected void code_consumed(Codes codes) {
        var code = codes.createFor(UserId.of(BOB),
                ClientId.of(THE_CLIENT_ID),
                SessionId.of(THE_SESSION_ID),
                REDIRECT_URI, SCOPES, UUID.randomUUID().toString());

        var userIdFoundBack = codes.consume(code, ClientId.of(THE_CLIENT_ID), REDIRECT_URI);
        assertThat(userIdFoundBack)
                .isPresent();

        var replay = codes.consume(code, ClientId.of(THE_CLIENT_ID), REDIRECT_URI);
        assertThat(replay)
                .isEmpty();
    }


    protected void wrongCode(Codes codes) {
        var code = codes.createFor(UserId.of(BOB),
                ClientId.of(THE_CLIENT_ID),
                SessionId.of(THE_SESSION_ID),
                REDIRECT_URI, SCOPES, UUID.randomUUID().toString());

        var userIdFoundBack = codes.consume(Code.of("WRONG"), ClientId.of(THE_CLIENT_ID), REDIRECT_URI);
        assertThat(userIdFoundBack)
                .isEmpty();
    }


    protected void wrong_client(Codes codes) {
        var code = codes.createFor(UserId.of(BOB),
                ClientId.of(THE_CLIENT_ID),
                SessionId.of(THE_SESSION_ID),
                REDIRECT_URI, SCOPES, UUID.randomUUID().toString());

        var userIdFoundBack = codes.consume(code, ClientId.of("ANOTHER_client_id"), REDIRECT_URI);
        assertThat(userIdFoundBack)
                .isEmpty();
    }

    protected void wrong_redirecturi(Codes codes) {
        var code = codes.createFor(UserId.of(BOB),
                ClientId.of(THE_CLIENT_ID),
                SessionId.of(THE_SESSION_ID), REDIRECT_URI, SCOPES, UUID.randomUUID().toString());

        var userIdFoundBack = codes.consume(code, ClientId.of(THE_CLIENT_ID), "http://zombiecool.su");
        assertThat(userIdFoundBack)
                .isEmpty();
    }


    protected void nullability(Codes codes) {
        assertThatThrownBy(() -> codes.createFor(null, null, null, null, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> codes.createFor(UserId.of(BOB), null, null, null, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> codes.createFor(null, ClientId.of(THE_CLIENT_ID), null, null, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> codes.createFor(null, null, null, REDIRECT_URI, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    protected void nullability_consume(Codes codes) {
        var code = codes.createFor(UserId.of(BOB),
                ClientId.of(THE_CLIENT_ID),
                SessionId.of(THE_SESSION_ID),
                REDIRECT_URI,
                SCOPES, UUID.randomUUID().toString());

        assertThatThrownBy(() -> codes.consume(null, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> codes.consume(code, null, REDIRECT_URI))
                .isInstanceOf(NullPointerException.class);

        assertThatThrownBy(() -> codes.consume(null, ClientId.of(THE_CLIENT_ID), REDIRECT_URI))
                .isInstanceOf(NullPointerException.class);
        assertThat(codes.consume(code, ClientId.of(THE_CLIENT_ID), null))
                .isEmpty();

        var userIdFoundBack = codes.consume(code, ClientId.of(THE_CLIENT_ID), REDIRECT_URI);
        assertThat(userIdFoundBack)
                .isNotEmpty();
    }
}
