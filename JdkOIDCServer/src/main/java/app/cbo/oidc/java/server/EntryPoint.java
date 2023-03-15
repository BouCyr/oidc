package app.cbo.oidc.java.server;

import java.io.IOException;

public class EntryPoint {

    public static void main(String... args) throws IOException {

        var parsedArgs = StartupArgs.from(args);
        var server = new Server(parsedArgs);
        server.start();


    }
}
