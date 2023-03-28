package app.cbo.oidc.java.server;

import java.io.IOException;
import java.util.logging.*;

public class EntryPoint {

    private final static Logger LOGGER = Logger.getLogger(EntryPoint.class.getCanonicalName());

    public static void main(String... args) throws IOException {


        Logger parent = Logger.getLogger("");
        parent.setLevel(Level.FINE);  // Loggers will now publish more messages.

        LOGGER.info("SAMPLE : http://localhost:9451/authorize?redirect_uri=http://www.google.fr&client_id=test&scope=openid&response_type=code");


        var parsedArgs = StartupArgs.from(args);
        var server = new Server(parsedArgs);
        server.start();


    }
}
