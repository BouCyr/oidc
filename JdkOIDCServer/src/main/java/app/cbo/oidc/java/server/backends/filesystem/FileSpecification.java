package app.cbo.oidc.java.server.backends.filesystem;

import java.util.List;

/**
 * This interface defines the specification for a file in the file system.
 * It provides methods to get the folders and the file name.
 * The folders method returns a list of folders that lead to the file.
 * The fileName method returns the name of the file.
 */
public interface FileSpecification {

    /**
     * This method returns a list of folders that lead to the file.
     * The folders are returned in the order from the root to the file.
     * Each string in the list is a name of a folder.
     *
     * @return a list of folder names from the root to the file.
     */
    List<String> folders();

    /**
     * This method returns the name of the file.
     *
     * @return the name of the file.
     */
    String fileName();
}