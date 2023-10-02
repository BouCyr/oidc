package app.cbo.oidc.java.server.backends.users;

import app.cbo.oidc.java.server.credentials.pwds.PBKDF2WithHmacSHA1PasswordHash;
import app.cbo.oidc.java.server.credentials.pwds.Passwords;
import app.cbo.oidc.java.server.datastored.user.User;
import app.cbo.oidc.java.server.datastored.user.UserId;
import org.assertj.core.api.Assertions;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class UsersTest {

    static Passwords passwords(){
        return new PBKDF2WithHmacSHA1PasswordHash();
    }

    static void testReadWrite(Users tested) {
        //cehck creation
        var userId = tested.create("login", "clear", "TOTPKEY");
        assertThat(userId).isNotNull();
        assertThat(userId.get()).isNotNull().isEqualTo("login");

        //cannot create an already existing user
        assertThatThrownBy(() -> tested.create("login", "???", "456456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(UserCreator.LOGIN_ALREADY_EXISTS);

        //cannot find an absent user
        assertThat(tested.find(UserId.of("blablabl"))).isEmpty();

        //find it back
        var found = tested.find(userId);
        assertThat(found).isPresent();


        var fromDisk = found.get();
        assertThat(fromDisk.sub()).isEqualTo("login");
        assertThat(fromDisk.totpKey()).isEqualTo("TOTPKEY");

        Assertions.assertThat(fromDisk.pwd()).isNotEqualTo("clear"); //pwd must have been hashed
        Assertions.assertThat(passwords().confront("clear", fromDisk.pwd())).isTrue();

        assertThat(fromDisk.consentedTo()).isEmpty();


        fromDisk.consentedTo().put("client1", Set.of("A", "B"));

        tested.update(fromDisk);


        assertThat(tested.update(new User(UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString())))
                .isFalse();

        var foundBack = tested.find(userId);
        assertThat(foundBack).isPresent();
        var updated = foundBack.get();
        assertThat(updated.sub()).isEqualTo("login");
        assertThat(updated.totpKey()).isEqualTo("TOTPKEY");

        Assertions.assertThat(updated.pwd()).isNotEqualTo("clear"); //pwd must have been hashed
        Assertions.assertThat(passwords().confront("clear", updated.pwd())).isTrue();
        assertThat(updated.pwd()).isNotEqualTo("clear"); //pwd must have been hashed
        assertThat(passwords().confront("clear", updated.pwd())).isTrue();

        assertThat(updated.consentedTo()).isNotEmpty()
                .hasSize(1).containsKey("client1");
        assertThat(updated.consentedTo().get("client1")).isNotEmpty()
                .hasSize(2)
                .containsExactlyInAnyOrderElementsOf(List.of("A", "B"));
    }
}
