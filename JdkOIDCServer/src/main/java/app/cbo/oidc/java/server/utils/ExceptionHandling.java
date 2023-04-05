package app.cbo.oidc.java.server.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

public class ExceptionHandling {

    private final static Logger LOGGER = Logger.getLogger(ExceptionHandling.class.getCanonicalName());

    public static String getStackTrace(Throwable e){
        try(
                var os = new ByteArrayOutputStream();
                var w = new PrintWriter(os)
        ) {
            e.printStackTrace(w);
            w.flush();
            return os.toString();
        } catch (IOException ioException) {
            LOGGER.info("Unable to read exception stacktrace :( This is an error inside an error");
            return "[STACKTRACE UNAVAILABLE]";
        }
    }
}
