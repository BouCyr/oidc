package app.cbo.oidc.java.server;

import app.cbo.oidc.java.server.backends.Users;
import app.cbo.oidc.java.server.datastored.User;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.*;

public class EntryPoint {

    private final static Logger LOGGER = Logger.getLogger(EntryPoint.class.getCanonicalName());

    public static void main(String... args) throws IOException {


        setupData();

        Logger parent = Logger.getLogger("");
        parent.setLevel(Level.FINE);  // Loggers will now publish more messages.

        LOGGER.info("SAMPLE : http://localhost:9451/authorize?redirect_uri=http://www.google.fr&client_id=test&scope=openid&response_type=code");


        var parsedArgs = StartupArgs.from(args);
        var server = new Server(parsedArgs);
        server.start();


    }

    @Deprecated
    //TODO [03/04/2023] read data on disk
    private static void setupData() {

        //String sub, String pwd, String totpKey, Map<String, Set<String>> consentedTo
        var cyrille = new User("cyrille","sesame", "ALBACORE", Collections.emptyMap());
        Users.getInstance().store(cyrille);
    }
}
