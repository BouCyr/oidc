package app.cbo.oidc.java.server.backends.users;

import app.cbo.oidc.java.server.backends.filesystem.FileSpecification;
import app.cbo.oidc.java.server.backends.filesystem.FileSpecifications;
import app.cbo.oidc.java.server.backends.filesystem.FileStorage;
import app.cbo.oidc.java.server.credentials.pwds.PasswordEncoder;
import app.cbo.oidc.java.server.datastored.user.User;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;
import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.utils.Utils;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static app.cbo.oidc.java.server.backends.filesystem.FileStorage.fromLine;
import static app.cbo.oidc.java.server.backends.filesystem.FileStorage.toLine;

@Injectable
public record FSUsers(FileStorage fsUserStorage, PasswordEncoder passwordEncoder) implements Users {

    private final static Logger LOGGER = Logger.getLogger(FSUsers.class.getCanonicalName());


    private static final String SUB_K = "sub";
    private static final String PWD_K = "pwd";
    private static final String TOTP_K = "totp";
    private static final String CONSENTS_K = "consents";


    @Override
    public UserId create(@NotNull String login, @Nullable String clearPwd, @Nullable String totpKey) {
        LOGGER.info("Checking if a user with this id is stored before creation");
        if (this.find(UserId.of(login)).isPresent()) {
            throw new RuntimeException(LOGIN_ALREADY_EXISTS);
        }
        if (Utils.isBlank(login)) {
            throw new IllegalArgumentException("Login is required.");
        }

        User newUser = new User(
                login,
                clearPwd != null ? this.passwordEncoder().encode(clearPwd) : null,
                totpKey);

        LOGGER.info("Writing credentials of new user '" + login + "' on disk");
        return writeUSer(newUser);
    }

    private UserId writeUSer(User newUser) {
        try (var writer = this.fsUserStorage.writer(this.fileOf(newUser.getUserId()))) {
            for (var dataLine : userToStrings(newUser)) {
                writer.write(dataLine);
                writer.newLine();
            }
        } catch (IOException e) {
            LOGGER.severe("IOException while writing user. This is not normal. " + e.getMessage());
            throw new RuntimeException(e);
        }
        return newUser.getUserId();
    }

    @NotNull
    @Override
    public Optional<User> find(@NotNull UserId userId) {
        LOGGER.info("Reading user data of #" + userId.get());

        try {
            var findFile = this.fsUserStorage.reader(this.fileOf(userId));
            if (findFile.isEmpty()) {
                return Optional.empty();
            }

            try (var reader = findFile.get()) {
                return userFromStrings(reader.lines().toList());
            }

        } catch (IOException e) {
            LOGGER.severe("IOException while reading user. This is not normal. " + e.getMessage());
            return Optional.empty();
        }

    }

    @Override
    public boolean update(@NotNull User user) {
        if (this.find(user.getUserId()).isEmpty()) {
            return false;
        }
        try {
            this.writeUSer(user);
            return true;
        } catch (Exception e) {
            LOGGER.severe("IOException while updating user. This is not normal. " + e.getMessage());
            return false;
        }
    }


    public static Collection<String> userToStrings(@NotNull User user) {
        return List.of(
                toLine(SUB_K, user.sub()),
                toLine(PWD_K, user.pwd()),
                toLine(TOTP_K, user.totpKey()),
                toLine(CONSENTS_K, consentsToString(user.consentedTo()))
        );

    }

    public static String consentsToString(Map<String, Set<String>> consentedTo) {
        // clientA->profile;email,clientB->email,phone,address
        return consentedTo.keySet()
                .stream()
                .map(clientId -> clientId + "->" + String.join(";", consentedTo.get(clientId)))
                .collect(Collectors.joining(","));
    }

    public static Optional<User> userFromStrings(@NotNull Collection<String> stringified) {
        if (Utils.isEmpty(stringified)) {
            return Optional.empty();
        }

        //String sub, String pwd, String totpKey, Map<String, Set<String>> consentedTo
        String sub = null;
        String pwd = null;
        String totpKey = null;
        Map<String, Set<String>> consents = null;

        for (var line : stringified) {
            var pair = fromLine(line);
            switch (pair.left()) {
                case SUB_K -> sub = pair.right();
                case PWD_K -> pwd = pair.right();
                case TOTP_K -> totpKey = pair.right();
                case CONSENTS_K -> consents = readStringConsents(pair.right());
                default -> LOGGER.info("unknown key found in file 'user.txt' : " + pair.right());
            }
        }

        if (sub != null)
            return Optional.of(new User(sub, pwd, totpKey, consents));
        else {
            LOGGER.warning("No userid/sub found in file");
            return Optional.empty();
        }
    }

    public static Map<String, Set<String>> readStringConsents(String val) {

        if (Utils.isBlank(val))
            return Collections.emptyMap();

        try {
            Map<String, Set<String>> map = new HashMap<>();
            var clientIds = val.split(",");
            for (var consentAndclient : clientIds) {
                var clientId = consentAndclient.split("->")[0];
                var consents = Set.of(consentAndclient.split("->")[1].split(";"));
                map.put(clientId, consents);
            }
            return map;
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Invalid consent string : '" + val + "'", e);
        }
    }

    public FileSpecification fileOf(UserId userId) {
        return FileSpecifications.forUser(userId).fileName("user.txt");

    }
}
