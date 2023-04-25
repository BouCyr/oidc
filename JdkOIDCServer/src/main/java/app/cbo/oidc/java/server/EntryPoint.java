package app.cbo.oidc.java.server;

import app.cbo.oidc.java.server.backends.Users;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntryPoint {

    private final static Logger LOGGER = Logger.getLogger(EntryPoint.class.getCanonicalName());

    public static void main(String... args) throws IOException {

        configureLogging();

        setupData();

        Logger parent = Logger.getLogger("");
        parent.setLevel(Level.FINE);  // Loggers will now publish more messages.

        LOGGER.info("SAMPLE : http://localhost:9451/authorize?redirect_uri=http://www.google.fr&client_id=test&scope=openid&response_type=code");


        var parsedArgs = StartupArgs.from(args);
        var server = new Server(parsedArgs);
        server.start();


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

                //TODO [24/04/2023] :
                //align date to a fixed length
                return LocalDateTime.ofInstant(logRecord.getInstant(), ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) +
                        " " + logRecord.getLevel() +
                        //find thread name ? align to fixed length
                        " thread#" + logRecord.getLongThreadID() +
                        //right align method name (with trncutaion to the left ?
                        " " + className +
                        //align to fixed length (truncate to the right)

                        "." + logRecord.getSourceMethodName() + "(...)" +
                        " : " + logRecord.getMessage() +
                        System.lineSeparator();

            }
        });
        mainLogger.addHandler(handler);
    }

    @Deprecated
    //TODO [03/04/2023] read data on disk
    private static void setupData() {


        Users.getInstance().create("cyrille", "sesame", "ALBACORE");
    }
}
