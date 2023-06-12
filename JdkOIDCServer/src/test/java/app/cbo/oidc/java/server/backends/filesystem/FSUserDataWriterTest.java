package app.cbo.oidc.java.server.backends.filesystem;


import app.cbo.oidc.java.server.datastored.user.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class FSUserDataWriterTest {

    private static final String[] HUGO = new String[]{
            "La plume, seul débris qui restât des deux ailes",
            "De l'archange englouti dans les nuits éternelles,",
            "Était toujours au bord du gouffre ténébreux."
    };
    private static final String[] HUGO2 = new String[]{
            "Quand il se fut assis sur sa chaise dans l'ombre",
            "Et qu'on eut sur son front fermé le souterrain,",
            "L'oeil était dans la tombe et regardait Caïn.",
    };


    Path folder;

    @BeforeEach
    void setup() throws IOException {
        folder = Files.createTempDirectory("testFs");
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
    void fileNotFound() throws IOException {
        var tested = new UserFileStorage(folder);
        var opt = tested.reader(() -> "bof", () -> "wat");

        assertThat(opt).isEmpty();

    }

    @Test
    void nominal() throws IOException {

        var tested = new UserFileStorage(folder);


        writeThenRead(tested, UserId.of("testUser"), () -> "monFichier", HUGO);
        writeThenRead(tested, UserId.of("testUser2"), () -> "monFichierA", HUGO);
        writeThenRead(tested, UserId.of("testUser3"), () -> "monFichierB", HUGO);
        writeThenRead(tested, UserId.of("testUser"), () -> "monFichier", HUGO2);
        writeThenRead(tested, UserId.of("testUser"), () -> "monFichierC", HUGO);
    }

    private void writeThenRead(UserFileStorage tested, UserId userId, fileSpecifications writeable, String... lyrics) throws IOException {

        //write
        try (var writer = tested.writer(userId, writeable)) {

            for (var verse : lyrics) {
                writer.write(verse);
                writer.newLine();
            }
        }

        assertThat(folder).exists()
                .isDirectory();
        var userFolder = folder.resolve(userId.getUserId());
        assertThat(userFolder).exists()
                .isDirectory();

        Path userFile;
        if (writeable.hasSubfolder()) {
            var subFolder = userFolder.resolve(writeable.subFolder());
            assertThat(subFolder).exists()
                    .isDirectory();

            userFile = subFolder.resolve(writeable.fileName());
        } else {
            userFile = userFolder.resolve(writeable.fileName());
        }
        assertThat(userFile)
                .exists()
                .isRegularFile()
                .isNotEmptyFile();

        String path = folder.toString() + File.separator
                + userId.getUserId() + File.separator
                + (writeable.hasSubfolder() ? writeable.subFolder() + File.separator : "")
                + writeable.fileName();
        assertThat(new File(path))
                .exists()
                .isFile()
                .isNotEmpty();

        //read
        var optReader = tested.reader(userId, writeable);

        if (optReader.isEmpty()) {
            fail("file not found");
            return;
        }

        try (var reader = optReader.get()) {
            for (var verse : lyrics) {
                assertThat(reader.readLine())
                        .isNotEmpty()
                        .isEqualTo(verse);
            }
        }
    }

    @Test
    void withSubFolder() throws IOException {

        var tested = new UserFileStorage(folder);

        fileSpecifications withSubFolder = new fileSpecifications() {
            @Override
            public String fileName() {
                return "monAutreFichier";
            }

            @Override
            public String subFolder() {
                return "sub";
            }
        };
        writeThenRead(tested, UserId.of("testUser"), withSubFolder, HUGO2);
        writeThenRead(tested, UserId.of("testUser"), withSubFolder, HUGO);
        writeThenRead(tested, UserId.of("testUser"), withSubFolder, HUGO2);
        writeThenRead(tested, UserId.of("anotherUser"), withSubFolder, HUGO2);
    }
}