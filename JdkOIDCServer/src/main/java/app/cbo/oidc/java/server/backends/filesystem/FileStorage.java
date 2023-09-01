package app.cbo.oidc.java.server.backends.filesystem;

import app.cbo.oidc.java.server.utils.Pair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Class that reads and writes data linked in the fileSystem
 */
public class FileStorage {


    private static final Logger LOGGER = Logger.getLogger(FileStorage.class.getCanonicalName());
    private final Path basePath;


    /**
     * Init
     *
     * @param basePath root path for user data
     * @throws IOException if root path is missing from the FS and cannot be created
     */
    public FileStorage(Path basePath) throws IOException {
        this.basePath = basePath;

        if (!Files.exists(basePath)) {
            Files.createDirectory(basePath);
        }
    }

    public static String toLine(String key, String val) {
        return key + ":" + val;
    }

    public static Pair<String, String> fromLine(String line) {
        var key = line.split(":")[0];
        try {
            var val = line.substring((key + ":").length());
            return Pair.of(key, val);
        } catch (IndexOutOfBoundsException e) {
            e.toString();
            throw e;
        }
    }


    public Optional<Map<String, String>> readMap(FileSpecification storageSpecs) throws IOException {
        var file = this.reader(storageSpecs);

        if (file.isEmpty()) {
            return Optional.empty();
        }

        try (var reader = file.get()) {
            return Optional.of(reader.lines()
                    .map(FileStorage::fromLine)
                    .collect(Collectors.toMap(Pair::left, Pair::right)));
        }
    }

    public void writeMap(FileSpecification storageSpecs, Map<String, String> record) throws IOException {
        try (var writer = this.writer(storageSpecs)) {
            for (var kv : record.entrySet()) {
                writer.write(toLine(kv.getKey(), kv.getValue()));
                writer.newLine();
            }
        }
    }

    /**
     * Opens a reader on some user info
     *
     * @param writeable definition of the data being read
     * @return A bufferedReader on the data, or EMPTY if the requested file cannot be found
     * @throws IOException when something goes wrong when opening the file. Please note that FileNotFound/NoSuchFileException will not be thrown, but will return Optional.empty instead
     */
    public Optional<BufferedReader> reader(FileSpecification writeable) throws IOException {

        try {
            var filePath = file(writeable);
            LOGGER.info("Opening reader on " + filePath.toString());
            return Optional.of(Files.newBufferedReader(filePath, StandardCharsets.UTF_8));
        } catch (FileNotFoundException | NoSuchFileException e) {
            LOGGER.info(String.format("File '%s' not found  %n", writeable.fileName()));
            return Optional.empty();
        }
    }

    /**
     * Opens a reader on some user info
     *
     * @param writeable definition of the data being read
     * @return A BufferedWriter on the data ; file will be created if missing
     * @throws IOException when something goes wrong when opening/creating the file
     */
    public BufferedWriter writer(FileSpecification writeable) throws IOException {
        var filePath = file(writeable);
        LOGGER.info("Opening writer on " + filePath.toString());
        return Files.newBufferedWriter(filePath, StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.SYNC, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public void delete(FileSpecification file) throws IOException {
        var filePath = file(file);
        LOGGER.info("Deleting " + filePath.toString());
        Files.delete(filePath);

    }

    /**
     * Find the complete path, including fileName and potential subfolder, for specific data linked to a user
     *
     * @param writeable definition of the data being read/written
     * @return path for the requested file
     * @throws IOException when something goes wrong when opening/creating the file
     */
    protected Path file(FileSpecification writeable) throws IOException {
        try {
            return directory(writeable).resolve(writeable.fileName());
        } catch (Exception e) {
            throw new IOException(e);
        }
    }


    /**
     * Find the folder path for specific data linked to a user (user folder and potential subfolder depending on the daata)
     *
     * @param writeable definition of the data being read/written
     * @return path for the requested file
     * @throws IOException when something goes wrong when opening/creating the file
     */
    protected Path directory(FileSpecification writeable) throws IOException {

        Path cur = basePath;

        for (String subFold : writeable.folders()) {

            cur = cur.resolve(subFold);

            if (!Files.exists(cur)) {
                Files.createDirectory(cur);
            }
        }


        return cur;
    }


}
