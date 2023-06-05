package app.cbo.oidc.java.server.backends.users;

import app.cbo.oidc.java.server.backends.filesystem.UserDataFileStorage;
import app.cbo.oidc.java.server.backends.filesystem.UserDataStorageSpecifications;
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

public class FSUsers implements UserFinder, UserCreator, UserUpdate {

    static final String USER_FILENAME = "user.txt";
    public static final UserDataStorageSpecifications USERWRITEABLE = () -> USER_FILENAME;
    static final String SUBK = "sub";
    static final String PWDK = "pwd";
    static final String TOTPK = "totp";
    static final String CONSENTSK = "consents";
    private final static Logger LOGGER = Logger.getLogger(FSUsers.class.getCanonicalName());
    private final UserDataFileStorage fsUserStorage;

    public FSUsers(UserDataFileStorage fsUserStorage) {
        this.fsUserStorage = fsUserStorage;
    }

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

        try {
            var findFile = this.fsUserStorage.reader(userId, USERWRITEABLE);
            if (findFile.isEmpty()) {
                return Optional.empty();
            }

            try (var reader = findFile.get()) {
                return this.userFromStrings(reader.lines().toList());
            }

        } catch (IOException e) {
            return Optional.empty();
        }

    }

    @Override
    public boolean update(@NotNull User user) {
        return false;
    }

    protected Collection<String> userToStrings(@NotNull User user) {
        return List.of(
                SUBK + ":" + user.sub(),
                PWDK + ":" + user.pwd(),
                TOTPK + ":" + user.totpKey(),
                CONSENTSK + ":" + this.consentsToString(user.consentedTo())
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
            var key = line.split(":")[0];
            var val = line.substring((key + ":").length());
            switch (key) {
                case SUBK:
                    sub = val;
                    break;
                case PWDK:
                    pwd = val;
                    break;
                case TOTPK:
                    totpKey = val;
                    break;
                case CONSENTSK:
                    consents = this.readStringConsents(val);
                    break;
                default:
                    LOGGER.info("unknown key found in file '" + USERWRITEABLE.fileName() + "' : " + key);
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
