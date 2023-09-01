package app.cbo.oidc.java.server.backends.codes;

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

class FsCodesTest extends CodesTest {

    Path folder;
    FileStorage storage;

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

    @Test
    void nominal() {

        nominal(new FSCodes(storage));
    }

    @Test
    void code_consumed() {
        code_consumed(new FSCodes(storage));
    }

    @Test
    void wrong_code() {

        wrongCode(new FSCodes(storage));
    }

    @Test
    void wrong_client() {

        wrong_client(new FSCodes(storage));
    }

    @Test
    void wrong_redirectUri() {

        wrong_redirecturi(new FSCodes(storage));
    }

    @Test
    void nullability_create() {

        nullability(new FSCodes(storage));
    }

    @Test
    void nullability_consume() {

        nullability_consume(new FSCodes(storage));
    }


}