package app.cbo.oidc.java.server.backends.filesystem;

import app.cbo.oidc.java.server.datastored.user.UserId;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Builder of FileSpecification
 * <p>
 * .for(userId = 84).fileName("user.txt") => {base}/84/user.txt
 * .for(userId = a654).in("claims").fileName("profile.txt") => {base}/a654/claims/profile.txt
 * .fileName("properties.ini") => {base}/properties.ini
 * .in("codes").fileName("45-54-65.txt") => {base}/codes/45-54-65.txt
 * .in("blabla","hmmm","wat").fileName("this.pdf") => {base}/blabla/hmmm/wat/this.pdf
 */
public class FileSpecifications {

    /**
     * Returns a FileSpecification for a file in subfolders
     *
     * @param fileName file name
     * @param folders  subFolders, may be empty
     * @return FileSpecification, ready to be consumed
     */
    public static FileSpecification full(String fileName, String... folders) {
        return new FileSpecification() {
            @Override
            public List<String> folders() {
                return Arrays.asList(folders);
            }

            @Override
            public String fileName() {
                return fileName;
            }
        };
    }

    /**
     * Returns a FileSepcfification for a file in subfolders
     *
     * @param fileName file name
     * @param folders  subFolders, may be empty
     * @return FileSpecification, ready to be consumed
     */
    public static FileSpecification full(String fileName, List<String> folders) {
        String[] asArr = new String[folders.size()];
        for (int i = 0; i < folders.size(); i++) {
            asArr[i] = folders.get(i);
        }
        return full(fileName, asArr);
    }

    /**
     * intermediate builder, allowing creation of a fileSepc for a user (meaning something in ./userId/...)
     */
    public static UserFile forUser(UserId userId) {
        return new UserFile(userId);
    }

    /**
     * Creates a FileSpec directly in the ROOT folder
     *
     * @param fileName fileName (e.g. 'toto.jpeg')
     * @return FS targeting ./fileName
     */
    public static FileSpecification fileName(String fileName) {
        return full(fileName);
    }

    /**
     * Creates a FileSpec directly in a subfolder folder
     * Warning, subfolder SHOULD NOT BE a userId
     *
     * @return FS targeting ./subfolder1/subfolder2/fileName
     */
    public static FileNamer in(String... subFolders) {
        return fileName -> full(fileName, subFolders);
    }

    /**
     * Functional interface for generating FileSpec (using the full(...) methods behind, I guess)
     */
    public interface FileNamer extends Function<String, FileSpecification> {

        default FileSpecification fileName(String fileName) {
            return this.apply(fileName);
        }

    }

    /**
     * intermediate builder type, allowing creation of a fileSepc for a user
     */
    public record UserFile(UserId userId) implements FileNamer {

        /**
         * Creates a FileSpec directly in the user folder
         *
         * @param fileName fileName (e.g. 'toto.jpeg')
         * @return FS targeting ./userId/fileName
         */
        public FileSpecification apply(String fileName) {
            return full(fileName, userId.getUserId());
        }

        /**
         * Intermediate build step for creating a FileSpec  in  a subfolder of the user folder
         *
         * @return FSBuilder targeting ./userId/subfolder1/subfolder2/??
         */
        public FileNamer in(String... subFolders) {
            List<String> folders = new ArrayList<>();
            folders.add(userId.getUserId());
            folders.addAll(Arrays.asList(subFolders));
            return fileName -> full(fileName, folders);

        }
    }


}
