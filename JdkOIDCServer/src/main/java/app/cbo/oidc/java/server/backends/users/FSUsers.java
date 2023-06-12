package app.cbo.oidc.java.server.backends.users;

import app.cbo.oidc.java.server.backends.filesystem.UserFileStorage;
import app.cbo.oidc.java.server.backends.filesystem.fileSpecifications;
import app.cbo.oidc.java.server.credentials.PasswordEncoder;
import app.cbo.oidc.java.server.datastored.user.User;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;
import app.cbo.oidc.java.server.utils.Utils;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static app.cbo.oidc.java.server.backends.filesystem.UserFileStorage.fromLine;
import static app.cbo.oidc.java.server.backends.filesystem.UserFileStorage.toLine;

public record FSUsers(UserFileStorage fsUserStorage) implements Users {

    private final static Logger LOGGER = Logger.getLogger(FSUsers.class.getCanonicalName());


    private static final String USER_FILENAME = "user.txt";
    public static final fileSpecifications USERWRITEABLE = () -> USER_FILENAME;
    private static final String SUB_K = "sub";
    private static final String PWD_K = "pwd";
    private static final String TOTP_K = "totp";
    private static final String CONSENTS_K = "consents";

    @Override
    public UserId create(@NotNull String login, @Nullable String clearPwd, @Nullable String totpKey) {
        LOGGER.info("Checking if a user with this id is stored before creation");
        if (this.find(UserId.of(login)).isPresent()) {
            throw new RuntimeException("Another user with this login already exists");
        }
        if (Utils.isBlank(login)) {
            throw new IllegalArgumentException("Login is required.");
        }

        User newUser = new User(
                login,
                clearPwd != null ? PasswordEncoder.getInstance().encodePassword(clearPwd) : null,
                totpKey);

        LOGGER.info("Writing credentials of new user '" + login + "' on disk");
        return writeUSer(newUser);
    }

    private UserId writeUSer(User newUser) {
        try (var writer = this.fsUserStorage.writer(newUser.getUserId(), USERWRITEABLE)) {
            for (var dataLine : this.userToStrings(newUser)) {
                writer.write(dataLine);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return newUser.getUserId();
    }

    @NotNull
    @Override
    public Optional<User> find(@NotNull UserId userId) {
        LOGGER.info("Reading user data of #" + userId.get());

        try {
            var findFile = this.fsUserStorage.reader(userId, USERWRITEABLE);
            if (findFile.isEmpty()) {
                return Optional.empty();
            }

            try (var reader = findFile.get()) {
                return this.userFromStrings(reader.lines().toList());
            }

        } catch (IOException e) {
            LOGGER.severe("IOException while reading user. This is not normal. " + e.getMessage());
            return Optional.empty();
        }

    }

    @Override
    public boolean update(@NotNull User user) {
        try {
            this.writeUSer(user);
            return true;
        } catch (Exception e) {
            LOGGER.severe("IOException while updating user. This is not normal. " + e.getMessage());
            return false;
        }
    }


    protected Collection<String> userToStrings(@NotNull User user) {
        return List.of(
                toLine(SUB_K, user.sub()),
                toLine(PWD_K, user.pwd()),
                toLine(TOTP_K, user.totpKey()),
                toLine(CONSENTS_K, this.consentsToString(user.consentedTo()))
        );

    }

    private String consentsToString(Map<String, Set<String>> consentedTo) {
        // clientA->profile;email,clientB->email,phone,address
        return consentedTo.keySet()
                .stream()
                .map(clientId -> clientId + "->" + String.join(";", consentedTo.get(clientId)))
                .collect(Collectors.joining(","));
    }

    protected Optional<User> userFromStrings(@NotNull Collection<String> stringified) {
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
                case CONSENTS_K -> consents = this.readStringConsents(pair.right());
                default -> LOGGER.info("unknown key found in file '" + USERWRITEABLE.fileName() + "' : " + pair.right());
            }
        }

        if (sub != null)
            return Optional.of(new User(sub, pwd, totpKey, consents));
        else {
            LOGGER.warning("No userid/sub found in file");
            return Optional.empty();
        }
    }

    private Map<String, Set<String>> readStringConsents(String val) {

        if (Utils.isBlank(val))
            return Collections.emptyMap();

        Map<String, Set<String>> map = new HashMap<>();
        var clientIds = val.split(",");
        for (var consentAndclient : clientIds) {
            var clientId = consentAndclient.split("->")[0];
            var consents = Set.of(consentAndclient.split("->")[1].split(";"));
            map.put(clientId, consents);
        }
        return map;
    }
}
