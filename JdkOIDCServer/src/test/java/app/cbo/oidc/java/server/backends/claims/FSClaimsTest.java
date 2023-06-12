package app.cbo.oidc.java.server.backends.claims;

import app.cbo.oidc.java.server.backends.filesystem.UserFileStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;


class FSClaimsTest {


    Path folder;
    UserFileStorage storage;

    @BeforeEach
    void setup() throws IOException {
        folder = Files.createTempDirectory("testFs");
        storage = new UserFileStorage(folder);
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

    @Test
    void testReadWrite() {
        //GIVEN
        var tested = new FSClaims(this.storage);

        ClaimsTest.testReadWrite(tested);
    }


}