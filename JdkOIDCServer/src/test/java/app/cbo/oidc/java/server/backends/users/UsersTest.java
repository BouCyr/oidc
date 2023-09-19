package app.cbo.oidc.java.server.backends.users;

import app.cbo.oidc.java.server.credentials.pwds.PBKDF2WithHmacSHA1PasswordHash;
import app.cbo.oidc.java.server.credentials.pwds.Passwords;
import org.assertj.core.api.Assertions;

import java.util.List;
import java.util.Set;

public class UsersTest {

    static Passwords passwords(){
        return new PBKDF2WithHmacSHA1PasswordHash();
    }

    static void testReadWrite(Users tested) {
        var userId = tested.create("login", "clear", "TOTPKEY");
        Assertions.assertThat(userId).isNotNull();
        Assertions.assertThat(userId.get()).isNotNull().isEqualTo("login");

        var found = tested.find(userId);
        Assertions.assertThat(found).isPresent();
        var fromDisk = found.get();
        Assertions.assertThat(fromDisk.sub()).isEqualTo("login");
        Assertions.assertThat(fromDisk.totpKey()).isEqualTo("TOTPKEY");

        Assertions.assertThat(fromDisk.pwd()).isNotEqualTo("clear"); //pwd must have been hashed
        Assertions.assertThat(passwords().confront("clear", fromDisk.pwd())).isTrue();

        Assertions.assertThat(fromDisk.consentedTo()).isEmpty();


        fromDisk.consentedTo().put("client1", Set.of("A", "B"));

        tested.update(fromDisk);

        var foundBack = tested.find(userId);
        Assertions.assertThat(foundBack).isPresent();
        var updated = foundBack.get();
        Assertions.assertThat(updated.sub()).isEqualTo("login");
        Assertions.assertThat(updated.totpKey()).isEqualTo("TOTPKEY");

        Assertions.assertThat(updated.pwd()).isNotEqualTo("clear"); //pwd must have been hashed
        Assertions.assertThat(passwords().confront("clear", updated.pwd())).isTrue();

        Assertions.assertThat(updated.consentedTo()).isNotEmpty()
                .hasSize(1).containsKey("client1");
        Assertions.assertThat(updated.consentedTo().get("client1")).isNotEmpty()
                .hasSize(2)
                .containsExactlyInAnyOrderElementsOf(List.of("A", "B"));
    }
}
