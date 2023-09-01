package app.cbo.oidc.java.server.backends.claims;

import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.datastored.user.claims.Address;
import app.cbo.oidc.java.server.datastored.user.claims.Mail;
import app.cbo.oidc.java.server.datastored.user.claims.Phone;
import app.cbo.oidc.java.server.datastored.user.claims.Profile;
import org.assertj.core.api.Assertions;

import java.util.Set;

public class ClaimsTest {

    static void testReadWrite(Claims tested) {
        var user = UserId.of("jeandidier");
        tested.store(new Phone(user, "monumero", true));
        tested.store(new Mail(user, "monemail", false));

        var retrieved = tested.claimsFor(user, Set.of("phone", "email"));

        Assertions.assertThat(retrieved)
                .isNotEmpty()
                .containsKey("phone")
                .containsKey("phone_verified")
                .containsKey("email")
                .containsKey("email_verified");

        Assertions.assertThat(retrieved).extractingByKey("phone").isEqualTo("monumero");
        Assertions.assertThat(retrieved).extractingByKey("phone_verified").isEqualTo(true);
        Assertions.assertThat(retrieved).extractingByKey("email").isEqualTo("monemail");
        Assertions.assertThat(retrieved).extractingByKey("email_verified").isEqualTo(false);

        tested.store(
                new Address(user, new Address.AddressPayload(
                        "17 place d'AAusterlitz, 59000 LILLE, NORD, FRANCE",
                        "17 place d'AAusterlitz",
                        "LILLE",
                        "NORD",
                        "59000",
                        "FRANCE")),
                new Profile(
                        user,
                        "Robert F. Johnson",
                        "Robert",
                        "Johnson",
                        "Franz",
                        "Bobby",
                        "BFJ",
                        "https://en.wikipedia.org/wiki/Robert_Johnson", //URL
                        "https://upload.wikimedia.org/wikipedia/en/thumb/b/b3/Robert_Johnson.png/220px-Robert_Johnson.png", //URL
                        "https://en.wikipedia.org/wiki/Robert_Johnson", //URL
                        "male",
                        "1911-05-08",
                        "us-east",
                        "en-EN",
                        421L
                ));

        retrieved = tested.claimsFor(user, Set.of("phone", "profile"));
        Assertions.assertThat(retrieved).extractingByKey("phone").isEqualTo("monumero");
        Assertions.assertThat(retrieved).extractingByKey("phone_verified").isEqualTo(true);
        Assertions.assertThat(retrieved).extractingByKey("given_name").isEqualTo("Robert");
        Assertions.assertThat(retrieved).extractingByKey("updated_at").isEqualTo(421L);

        retrieved = tested.claimsFor(user, Set.of("phone", "profile", "email", "address"));
        Assertions.assertThat(retrieved).extractingByKey("phone").isEqualTo("monumero");
        Assertions.assertThat(retrieved).extractingByKey("phone_verified").isEqualTo(true);
        Assertions.assertThat(retrieved).extractingByKey("given_name").isEqualTo("Robert");
        Assertions.assertThat(retrieved).extractingByKey("updated_at").isEqualTo(421L);
        Assertions.assertThat(retrieved)
                .containsKey("address")
                .extractingByKey("address").isInstanceOf(String.class);
    }
}
