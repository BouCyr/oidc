package app.cbo.oidc.java.server.backends.filesystem;

import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.utils.Pair;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Class that reads and writes data linked to a user in the fileSystem
 */
public class UserDataFileStorage {


    private static final Logger LOGGER = Logger.getLogger(UserDataFileStorage.class.getCanonicalName());
    private final Path basePath;


    /**
     * Init
     *
     * @param basePath root path for user data
     * @throws IOException if root path is missing from the FS and cannot be created
     */
    public UserDataFileStorage(Path basePath) throws IOException {
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

    /**
     * Returns the canonical constructor of a record class
     *
     * @param recordClass the record class
     * @param <T>         the record type
     * @return the canonical constructor
     */
    public static <T extends Record> Constructor<T> constructorOf(Class<T> recordClass) {
        Class<?>[] componentTypes = Arrays.stream(recordClass.getRecordComponents())
                .map(RecordComponent::getType)
                .toArray(Class<?>[]::new);
        try {
            return recordClass.getDeclaredConstructor(componentTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Reflection error... ");
        }
    }

    public Optional<Map<String, String>> readMap(UserId userId, UserDataStorageSpecifications storageSpecs) throws IOException {
        var file = this.reader(userId, storageSpecs);

        if (file.isEmpty()) {
            return Optional.empty();
        }

        try (var reader = file.get()) {
            return Optional.of(reader.lines()
                    .map(UserDataFileStorage::fromLine)
                    .collect(Collectors.toMap(Pair::left, Pair::right)));
        }
    }

    public void writeMap(UserId userId, UserDataStorageSpecifications storageSpecs, Map<String, String> record) throws IOException {
        try (var writer = this.writer(userId, storageSpecs)) {
            for (var kv : record.entrySet()) {
                writer.write(toLine(kv.getKey(), kv.getValue()));
                writer.newLine();
            }
        }
    }

    /**
     * Opens a reader on some user info
     *
     * @param userId    the user we want to read data for
     * @param writeable definition of the data being read
     * @return A bufferedReader on the data, or EMPTY if the requested file cannot be found
     * @throws IOException when something goes wrong when opening the file. Please note that FileNotFound/NoSuchFileException will not be thrown, but will return Optional.empty instead
     */
    public Optional<BufferedReader> reader(UserId userId, UserDataStorageSpecifications writeable) throws IOException {

        try {
            return Optional.of(Files.newBufferedReader(file(userId, writeable), StandardCharsets.UTF_8));
        } catch (FileNotFoundException | NoSuchFileException e) {
            LOGGER.info(String.format("File '%s' not found for user with id '%s' %n", writeable.fileName(), userId.getUserId()));
            return Optional.empty();
        }
    }

    /**
     * Opens a reader on some user info
     *
     * @param userId    the user we want to read data for
     * @param writeable definition of the data being read
     * @return A BufferedWriter on the data ; file will be created if missing
     * @throws IOException when something goes wrong when opening/creating the file
     */
    public BufferedWriter writer(UserId userId, UserDataStorageSpecifications writeable) throws IOException {
        return Files.newBufferedWriter(file(userId, writeable), StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.SYNC, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Find the complete path, including fileName and potential subfolder, for specific data linked to a user
     *
     * @param userId    the user we want to read data for
     * @param writeable definition of the data being read/written
     * @return path for the requested file
     * @throws IOException when something goes wrong when opening/creating the file
     */
    protected Path file(UserId userId, UserDataStorageSpecifications writeable) throws IOException {
        return directory(userId, writeable).resolve(writeable.fileName());
    }

    /**
     * Find the folder path for specific data linked to a user (user folder and potential subfolder depending on the daata)
     *
     * @param userId    the user we want to read data for
     * @param writeable definition of the data being read/written
     * @return path for the requested file
     * @throws IOException when something goes wrong when opening/creating the file
     */
    protected Path directory(UserId userId, UserDataStorageSpecifications writeable) throws IOException {
        var userDirectory = basePath.resolve(userId.getUserId());
        if (!Files.exists(userDirectory)) {
            Files.createDirectory(userDirectory);
        }

        if (!writeable.hasSubfolder()) {
            return userDirectory;
        }
        var dataFolder = userDirectory.resolve(writeable.subFolder());
        if (!Files.exists(dataFolder)) {
            Files.createDirectory(dataFolder);
        }
        return dataFolder;
    }

}
