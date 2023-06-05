package app.cbo.oidc.java.server.backends.users;

import app.cbo.oidc.java.server.backends.filesystem.UserDataFileStorage;
import app.cbo.oidc.java.server.credentials.PasswordEncoder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;

class FSUsersTest {

    Path folder;
    UserDataFileStorage storage;


    @Test
    void testReadWrite() {
        //GIVEN
        var tested = new FSUsers(this.storage);

        //WHEN
        var userId = tested.create("login", "clear", "TOTPKEY");
        Assertions.assertThat(userId).isNotNull();
        Assertions.assertThat(userId.get()).isNotNull().isEqualTo("login");

        var found = tested.find(userId);
        Assertions.assertThat(found).isPresent();
        var fromDisk = found.get();
        Assertions.assertThat(fromDisk.sub()).isEqualTo("login");
        Assertions.assertThat(fromDisk.totpKey()).isEqualTo("TOTPKEY");

        Assertions.assertThat(fromDisk.pwd()).isNotEqualTo("clear"); //pwd must have been hashed
        Assertions.assertThat(PasswordEncoder.getInstance().confront("clear", fromDisk.pwd())).isTrue();

        Assertions.assertThat(fromDisk.consentedTo()).isEmpty();


        fromDisk.consentedTo().put("client1", Set.of("A", "B"));

        tested.update(fromDisk);

        var foundBack = tested.find(userId);
        Assertions.assertThat(foundBack).isPresent();
        var updated = foundBack.get();
        Assertions.assertThat(updated.sub()).isEqualTo("login");
        Assertions.assertThat(updated.totpKey()).isEqualTo("TOTPKEY");

        Assertions.assertThat(updated.pwd()).isNotEqualTo("clear"); //pwd must have been hashed
        Assertions.assertThat(PasswordEncoder.getInstance().confront("clear", updated.pwd())).isTrue();

        Assertions.assertThat(updated.consentedTo()).isNotEmpty()
                .hasSize(1).containsKey("client1");
        Assertions.assertThat(updated.consentedTo().get("client1")).isNotEmpty()
                .hasSize(2)
                .containsExactlyInAnyOrderElementsOf(List.of("A", "B"));


    }


    @BeforeEach
    void setup() throws IOException {
        folder = Files.createTempDirectory("testFs");
        storage = new UserDataFileStorage(folder);
    }

    @AfterEach
    void teardown() throws IOException {
        Files.walkFileTree(folder, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}