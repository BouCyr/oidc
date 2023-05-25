package app.cbo.oidc.java.server;

import app.cbo.oidc.java.server.backends.claims.ClaimsStorer;
import app.cbo.oidc.java.server.backends.users.UserCreator;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.datastored.user.claims.Address;
import app.cbo.oidc.java.server.datastored.user.claims.Mail;
import app.cbo.oidc.java.server.datastored.user.claims.Phone;
import app.cbo.oidc.java.server.datastored.user.claims.Profile;
import app.cbo.oidc.java.server.deps.DependenciesBuilder;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntryPoint {

    private final static Logger LOGGER = Logger.getLogger(EntryPoint.class.getCanonicalName());

    public static void main(String... args) throws IOException {


        LOGGER.info("Starting");
        long start = System.nanoTime();
        LOGGER.info("Configuring logging");
        configureLogging();
        LOGGER.info("Configured logging");
        var parsedArgs = StartupArgs.from(args);
        LOGGER.info("Building dependencies");
        var dependencies = new DependenciesBuilder(parsedArgs);
        LOGGER.info("Dependencies built");
        setupData(dependencies.users(), dependencies.claims());
        LOGGER.info("Sample data built");
        var server = dependencies.server();
        LOGGER.info("Starting server");
        server.start();
        LOGGER.info("Started in " + Duration.ofNanos(System.nanoTime() - start).toMillis() + "ms");

    }

    private static void configureLogging() {

        //shorten the app.cbo.... package name when present.
        String basePackageFull = EntryPoint.class.getPackageName();
        var basePackageShort = Stream.of(basePackageFull.split("\\."))
                .map(pkgLevel -> pkgLevel.substring(0, 1))
                .collect(Collectors.joining("."));

        Logger mainLogger = Logger.getLogger("app.cbo.oidc.java.server");
        mainLogger.setUseParentHandlers(false);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter() {
            @Override
            public synchronized String format(LogRecord logRecord) {

                //shorten the app.cbo.... package name when present.
                String className = logRecord.getSourceClassName();
                if (className.startsWith(basePackageFull)) {
                    className = basePackageShort + className.substring(basePackageFull.length());
                }

                //TODO [24/04/2023] use String format
                //TODO [24/04/2023] : align date to a fixed length
                return LocalDateTime.ofInstant(logRecord.getInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) +
                        " " + logRecord.getLevel() +
                        //TODO find thread name ? align to fixed length
                        " thread#" + logRecord.getLongThreadID() +
                        //TODO right align method name (with truncate to the left ?)
                        " " + className +
                        //TODO align to fixed length (truncate to the right)

                        "." + logRecord.getSourceMethodName() + "(...)" +
                        " : " + logRecord.getMessage() +
                        System.lineSeparator();

            }
        });
        mainLogger.addHandler(handler);
    }

    @Deprecated
    //TODO [03/04/2023] read data on disk
    private static void setupData(UserCreator userCreator, ClaimsStorer claimsStorer) {


        var uid = UserId.of("cyrille");
        LOGGER.info("Creating user");
        userCreator.create(uid.getUserId(), "sesame", "ALBACORE");

        LOGGER.info("Creating user data");
        Phone phone = new Phone(uid, "0682738532", false);
        Mail mail = new Mail(uid, "cyrille@example.com", false);
        Address address = new Address(uid, "17 place de la République, 59000 Lille, NORD, FRANCE", "17 place de la République", "LILLE", "NORD", "59000", "FRANCE");
        Profile profile = new Profile(
                uid,
                "Cyrille BOUCHER",
                "Cyrille",
                "BOUCHER",
                "Charles",
                "cbo",
                "cbo@cbo.app",
                "http://profile.cbo.app/me", //URL
                "http://profile.cbo.app/picture", //URL
                "http://profile.cbo.app/", //URL
                "mind your business",
                "1982-11-29",
                "Europe/Paris",
                "fr-FR",
                LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        );
        claimsStorer.store(phone, mail, address, profile);
        LOGGER.info("All data created & stored");
    }
}
