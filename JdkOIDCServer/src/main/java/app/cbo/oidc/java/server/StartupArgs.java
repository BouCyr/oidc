package app.cbo.oidc.java.server;

import app.cbo.oidc.java.server.jsr305.Nullable;
import app.cbo.oidc.java.server.utils.Pair;
import app.cbo.oidc.java.server.utils.Utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Stream;

@Deprecated(forRemoval = true)
/**
 * @deprecated User ProgramArgs and ne wDI engine instead
 */
public record StartupArgs(int port, boolean fsBackEnd, @Nullable Path basePath) {

    public static final String PATH_ARGS = "path";

    public static final String PORT_ARGS = "port";
    public static final String BACKEND_ARGS = "backend";
    private static final Logger LOGGER = Logger.getLogger(StartupArgs.class.getCanonicalName());

    private final static Set<String> ARGS = Set.of(PORT_ARGS, BACKEND_ARGS);

    StartupArgs(String port, boolean isFileStorage, Path storagePath) {


        this(Integer.parseInt(port),
                isFileStorage,
                storagePath);
    }

    public static StartupArgs from(String... array) {

        //TODO [15/03/2023] case port=6=5
        //TODO [15/03/2023] print help if invalid args...

        final var asMap = new HashMap<String, String>();
        Stream.of(array)
                .map(s -> s.split("="))
                .map(split -> Pair.of(split[0], split[1]))
                .filter(kv -> ARGS.contains(kv.left()))
                .forEach(kv -> asMap.put(kv.left(), kv.right()));

        var isFileStorage = !"mem".equals(asMap.getOrDefault(BACKEND_ARGS, "file"));
        return new StartupArgs(
                asMap.getOrDefault(PORT_ARGS, "9451"),
                isFileStorage,
                isFileStorage ? storageFolder(asMap.get(PATH_ARGS)) : null
        );
    }


    public static Path storageFolder(@Nullable final String pathArg) {
        return storageFolder(pathArg, () -> System.getProperty("os.name"));
    }

    public static Path storageFolder(@Nullable final String pathArg, Supplier<String> osStringSupplier) {


        String folder = pathArg;
        if (Utils.isBlank(folder)) {

            String operatingSystem = (osStringSupplier.get()).toLowerCase(Locale.ROOT);

            if (operatingSystem.contains("win")) {
                //windows => AppData, or maybe roaming, whatever
                folder = System.getenv("AppData");
            } else {
                //unix/linux => home folder
                folder = System.getProperty("user.home");

                // TODO [19/06/2023] should check when running on other OS (at least MacOs, iOs and Android would be nice)
            }
            folder += File.separator + "oidc";

        }

        Path storageFolder = Path.of(folder);
        if (!Files.exists(Path.of(folder)) || !Files.isDirectory(Path.of(folder))) {
            throw new RuntimeException("Storage folder found does not exist)");
        }
        return storageFolder;
    }
}
