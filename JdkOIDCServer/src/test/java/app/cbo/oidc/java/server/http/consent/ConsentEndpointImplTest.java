package app.cbo.oidc.java.server.http.consent;

import app.cbo.oidc.java.server.credentials.AuthenticationMode;
import app.cbo.oidc.java.server.datastored.OngoingAuthId;
import app.cbo.oidc.java.server.datastored.Session;
import app.cbo.oidc.java.server.datastored.user.User;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.http.AuthErrorInteraction;
import app.cbo.oidc.java.server.http.authorize.AuthorizeParams;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

class ConsentEndpointImplTest {

    @Test
    void give_consent() throws AuthErrorInteraction {

        ConsentEndpointImpl tested = new ConsentEndpointImpl(
                p -> OngoingAuthId.of("oai"),
                userId -> Optional.of(new User("user", "pwd", "totp")),
                user -> true
        );

        //client asked for "my_perso_info", consent is given
        Session session = new Session(UserId.of("user"), EnumSet.of(AuthenticationMode.DECLARATIVE));
        var interaction = tested.treatRequest(Optional.of(session),
                new ConsentParams(
                        Set.of("my_perso_info"),
                        true,
                        "CLIENT",
                        new AuthorizeParams(Map.of("scope", List.of("my_perso_info"))),
                        true
                ));


        //consent was given
        Assertions.assertThat(interaction)
                .isInstanceOf(ConsentGivenInteraction.class);
    }

    @Test
    void missing_consent() throws AuthErrorInteraction {

        ConsentEndpointImpl tested = new ConsentEndpointImpl(
                p -> OngoingAuthId.of("oai"),
                userId -> Optional.of(new User("user", "pwd", "totp")),
                user -> true
        );

        Session session = new Session(UserId.of("user"), EnumSet.of(AuthenticationMode.DECLARATIVE));

        //client asked for "my_perso_info" & "my_very_perso_info", consent is given only for my_perso_info""
        var interaction = tested.treatRequest(Optional.of(session),
                new ConsentParams(
                        Set.of("my_perso_info"),
                        true,
                        "CLIENT",
                        new AuthorizeParams(Map.of("scope", List.of("my_perso_info", "my_very_perso_info"))),
                        true
                ));


        //consent was given
        Assertions.assertThat(interaction)
                .isInstanceOf(ConsentGivenInteraction.class);
    }
}