package app.cbo.oidc.java.server.backends.filesystem;

import java.util.List;

/**
 * Defines how (and where) data should be written : filename, subfolder if needed
 */
public interface FileSpecification {

    List<String> folders();

    String fileName();
}
