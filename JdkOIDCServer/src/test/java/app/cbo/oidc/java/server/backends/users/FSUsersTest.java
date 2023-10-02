package app.cbo.oidc.java.server.backends.users;

import app.cbo.oidc.java.server.backends.filesystem.FileStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FSUsersTest {

    Path folder;
    FileStorage storage;


    @Test
    void testReadWrite() {
        //GIVEN
        var tested = new FSUsers(this.storage, UsersTest.passwords());

        //WHEN
        UsersTest.testReadWrite(tested);
    }


    @Test
    void unknownFileContent() {
    }

    @Test
    void nominal() {
        var list = List.of("sub:login2", "pwd:clear", "totp:TOTPKEY", "consents:c1->s11;s12,c2->s21;22;s23");
        var ok = FSUsers.userFromStrings(list);
        assertThat(ok).isPresent()
                .get().isNotNull();
        var user = ok.get();
        assertThat(user.sub()).isEqualTo("login2");
        assertThat(user.pwd()).isEqualTo("clear");
        assertThat(user.totpKey()).isEqualTo("TOTPKEY");
        assertThat(user.consentedTo()).isNotEmpty();
    }

    @Test
    void invalid_consents_separator() {
        //consent separator is ',' instead of ';'
        var list = List.of("sub:login2", "pwd:clear", "totp:TOTPKEY", "consents:c1->s11;s12,c2->s21,s22,s23");
        assertThatThrownBy(() -> FSUsers.userFromStrings(list))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Invalid consent string : '")
                .hasRootCauseInstanceOf(RuntimeException.class);
    }

    @Test
    void consents_empty() {
        var list = List.of("sub:login2", "pwd:clear", "totp:TOTPKEY", "consents:");
        var ok = FSUsers.userFromStrings(list);
        assertThat(ok).isPresent()
                .get().isNotNull();
        var user = ok.get();
        assertThat(user.sub()).isEqualTo("login2");
        assertThat(user.pwd()).isEqualTo("clear");
        assertThat(user.totpKey()).isEqualTo("TOTPKEY");
        assertThat(user.consentedTo()).isEmpty();
    }

    @Test
    void unknonw_field() {
        //with an extra key
        var list = List.of("sub:login2", "pwd:clear", "totp:TOTPKEY", "consents:", "blablabla:blublublu");
        var ok = FSUsers.userFromStrings(list);
        assertThat(ok).isPresent()
                .get().isNotNull();
        var user = ok.get();
        assertThat(user.sub()).isEqualTo("login2");
        assertThat(user.pwd()).isEqualTo("clear");
        assertThat(user.totpKey()).isEqualTo("TOTPKEY");
        assertThat(user.consentedTo()).isEmpty();
    }

    @Test
    void no_consents() {
        var list = List.of("sub:login2", "pwd:clear", "totp:TOTPKEY");
        var ok = FSUsers.userFromStrings(list);
        assertThat(ok)
                .isPresent()
                .get().isNotNull();
        var user = ok.get();
        assertThat(user.sub()).isEqualTo("login2");
        assertThat(user.pwd()).isEqualTo("clear");
        assertThat(user.totpKey()).isEqualTo("TOTPKEY");
        assertThat(user.consentedTo()).isEmpty();
    }

    @Test
    void no_login() {

        var list = List.of("pwd:clear", "totp:TOTPKEY");
        var ok = FSUsers.userFromStrings(list);
        assertThat(ok).isEmpty();
    }

    @Test
    void empty() {
        List<String> list = Collections.emptyList();
        var ok = FSUsers.userFromStrings(list);
        assertThat(ok).isEmpty();
    }

    @Test
    void input_null() {
        var ok = FSUsers.userFromStrings(null);
        assertThat(ok).isEmpty();
    }

    @BeforeEach
    void setup() throws IOException {
        folder = Files.createTempDirectory("testFs");
        storage = new FileStorage(folder);
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