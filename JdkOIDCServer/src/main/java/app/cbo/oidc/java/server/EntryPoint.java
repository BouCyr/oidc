package app.cbo.oidc.java.server;

import app.cbo.oidc.java.server.backends.claims.Claims;
import app.cbo.oidc.java.server.backends.claims.ClaimsStorer;
import app.cbo.oidc.java.server.backends.clients.ClientRegistry;
import app.cbo.oidc.java.server.backends.users.Users;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.datastored.user.claims.Address;
import app.cbo.oidc.java.server.datastored.user.claims.Mail;
import app.cbo.oidc.java.server.datastored.user.claims.Phone;
import app.cbo.oidc.java.server.datastored.user.claims.Profile;
import app.cbo.oidc.java.server.scan.exceptions.DownStreamException;
import app.cbo.oidc.java.server.scan.props.PropsProviders;
import app.cbo.oidc.java.server.utils.Pair;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntryPoint {

    private final static Logger LOGGER = Logger.getLogger(EntryPoint.class.getCanonicalName());


    public static void main(String... args) throws IOException, DownStreamException {

        LOGGER.info("Starting");
        long start = System.nanoTime();

        configureLogging();

        //TODO [a118608][15/03/2024] Read the profile from the command line before starting the scanner
//        var scanner = new app.cbo.oidc.java.server.scan.Scanner("KEYCLOAK", "app.cbo.oidc.java.server")

        //scan the classpath for the server and its dependencies
        var scanner = new app.cbo.oidc.java.server.scan.Scanner("app.cbo.oidc.java.server")
                //default
                .withProperties(
                        List.of(
                                Pair.of("basePath", "c:\\work\\OIDC"),
                                Pair.of("port", "9451"),
                                Pair.of("domain", "http://localhost")))
                //overrides with command line args
                .withProperties(PropsProviders.fromArgs(args));

        setupUser("Cyrille", scanner.get(Users.class), scanner.get(Claims.class));
        setupUser("Marion", scanner.get(Users.class), scanner.get(Claims.class));
        setUpClient("sb","sbSecret", scanner.get(ClientRegistry.class));

        //get root class (server)
        var server = scanner.get(OIDCServer.class);
        LOGGER.info("Starting server");
        server.start();
        LOGGER.info("Started in " + Duration.ofNanos(System.nanoTime() - start).toMillis() + "ms");
    }

    private static void configureLogging() {

        Logger mainLogger = Logger.getLogger("app.cbo.oidc.java.server");
        mainLogger.setUseParentHandlers(false);
        mainLogger.setLevel(Level.FINEST);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new LogFormatter());
        mainLogger.addHandler(handler);
    }

    public static class LogFormatter extends SimpleFormatter {

        private final String basePackageFull;
        private final String basePackageShort;

        public LogFormatter() {
            //shorten the app.cbo.... package name when present.
            this.basePackageFull = EntryPoint.class.getPackageName();
            this.basePackageShort = Stream.of(basePackageFull.split("\\."))
                    .map(pkgLevel -> pkgLevel.substring(0, 1))
                    .collect(Collectors.joining("."));
        }

        @Override
        public synchronized String format(LogRecord logRecord) {

            //shorten the app.cbo.... package name when present.
            String className = logRecord.getSourceClassName();
            if (className.startsWith(basePackageFull)) {
                className = basePackageShort + className.substring(basePackageFull.length());
            }

            var dtt = LocalDateTime.ofInstant(logRecord.getInstant(), ZoneId.systemDefault()).format(
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            return
                    "[" + dtt + "]" +
                            "[" + logRecord.getLevel() + "]" +
                            "[thread#" + logRecord.getLongThreadID() + "]" +
                            "[" + className +
                            "." + logRecord.getSourceMethodName() + "]" +
                            " : " + logRecord.getMessage() +
                            System.lineSeparator();

        }
    }


    private static void setUpClient(String clientId, String secret, ClientRegistry clientRegistry){
        clientRegistry.setClient(clientId, secret);
    }

    private static void setupUser(String firstName, Users users, ClaimsStorer claimsStorer) {
        var uid = UserId.of(firstName.toLowerCase(Locale.ROOT));

        if (users.find(uid).isPresent()) {
            return;
        }

        LOGGER.info("Creating user");
        users.create(uid.getUserId(), "sesame", "ALBACORE");

        LOGGER.info("Creating user data");
        Phone phone = new Phone(uid, "0682738532", false);
        Mail mail = new Mail(uid, firstName.toLowerCase(Locale.ROOT) + "@example.com", false);
        Address address = new Address(uid, "17 place de la République, 59000 Lille, NORD, FRANCE", "17 place de la République", "LILLE", "NORD", "59000", "FRANCE");
        Profile profile = new Profile(
                uid,
                firstName + " BOUCHER",
                firstName,
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
