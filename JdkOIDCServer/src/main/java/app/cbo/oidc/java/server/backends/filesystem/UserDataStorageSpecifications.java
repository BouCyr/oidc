package app.cbo.oidc.java.server.backends.filesystem;

import app.cbo.oidc.java.server.utils.Utils;

/**
 * Defines how (and where) data should be written : filename, subfolder if needed
 */
@FunctionalInterface
public interface UserDataStorageSpecifications {

    String fileName();

    default String subFolder() {
        return "";
    }

    default boolean hasSubfolder() {
        return !Utils.isBlank(subFolder());
    }
}
