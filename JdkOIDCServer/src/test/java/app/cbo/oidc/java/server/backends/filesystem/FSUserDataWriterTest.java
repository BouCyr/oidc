package app.cbo.oidc.java.server.backends.filesystem;


import app.cbo.oidc.java.server.datastored.user.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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


    Path testBase;

    @BeforeEach
    void setup() throws IOException {
        testBase = Files.createTempDirectory("testFs");
    }

    @AfterEach
    void teardown() throws IOException {
        Files.walkFileTree(testBase, new SimpleFileVisitor<>() {
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
        var tested = new FileStorage(testBase);
        var opt = tested.reader(FileSpecifications.fileName("bof"));

        assertThat(opt).isEmpty();

    }

    @Test
    void nominal() throws IOException {

        var tested = new FileStorage(testBase);


        var file = FileSpecifications.forUser(UserId.of("testUser")).fileName("monFichier");

        writeThenRead(tested, UserId.of("testUser"), FileSpecifications.forUser(UserId.of("testUser")).fileName("monFichier"), HUGO);
        writeThenRead(tested, UserId.of("testUser2"), FileSpecifications.forUser(UserId.of("testUser2")).fileName("monFichierA"), HUGO);
        writeThenRead(tested, UserId.of("testUser2"), FileSpecifications.forUser(UserId.of("testUser2")).fileName("monFichierA"), HUGO);
        writeThenRead(tested, UserId.of("testUser3"), FileSpecifications.forUser(UserId.of("testUser3")).fileName("monFichierB"), HUGO);
        writeThenRead(tested, UserId.of("testUser"), FileSpecifications.forUser(UserId.of("testUser")).fileName("monFichier"), HUGO2);
        writeThenRead(tested, UserId.of("testUser"), FileSpecifications.forUser(UserId.of("testUser")).fileName("monFichierC"), HUGO);
        writeThenRead(tested, UserId.of("testUser"), FileSpecifications.forUser(UserId.of("testUser")).fileName("monFichierB"), HUGO);

    }

    private void writeThenRead(FileStorage tested, UserId userId, FileSpecification writeable, String... lyrics) throws IOException {

        //write
        try (var writer = tested.writer(writeable)) {

            for (var verse : lyrics) {
                writer.write(verse);
                writer.newLine();
            }
        }

        assertThat(testBase).exists()
                .isDirectory();

        Path userFile = testBase;
        for (String folder : writeable.folders()) {
            userFile = userFile.resolve(folder);
            assertThat(userFile).exists()
                    .isDirectory();


        }
        userFile = userFile.resolve(writeable.fileName());
        assertThat(userFile)
                .exists()
                .isRegularFile()
                .isNotEmptyFile();



        //read
        var optReader = tested.reader(writeable);

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

        var tested = new FileStorage(testBase);


        FileSpecification withSubFolder = FileSpecifications
                .forUser(UserId.of("testUser"))
                .in("sub")
                .fileName("monAutreFichier");

        writeThenRead(tested, UserId.of("testUser"), withSubFolder, HUGO2);
        writeThenRead(tested, UserId.of("testUser"), withSubFolder, HUGO);
        writeThenRead(tested, UserId.of("testUser"), withSubFolder, HUGO2);
        withSubFolder = FileSpecifications.forUser(UserId.of("anotherUser")).in("sub").fileName("monAutreFichier");
        writeThenRead(tested, UserId.of("anotherUser"), withSubFolder, HUGO2);
    }
}