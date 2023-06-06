package app.cbo.oidc.java.server.backends.claims;

import app.cbo.oidc.java.server.backends.filesystem.UserDataFileStorage;
import app.cbo.oidc.java.server.backends.filesystem.UserDataStorageSpecifications;
import app.cbo.oidc.java.server.backends.users.FSUsers;
import app.cbo.oidc.java.server.datastored.user.UserId;
import app.cbo.oidc.java.server.datastored.user.claims.*;
import app.cbo.oidc.java.server.utils.Utils;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

public record FSClaims(UserDataFileStorage fsUserStorage) implements Claims {


    private final static Logger LOGGER = Logger.getLogger(FSUsers.class.getCanonicalName());


    private UserDataStorageSpecifications fsSpecs(ScopedClaims scopedClaims) {
        return fsSpecs(scopedClaims.getClass());
    }

    private UserDataStorageSpecifications fsSpecs(Class<? extends ScopedClaims> scopedClaimType) {
        return new UserDataStorageSpecifications() {
            @Override
            public String fileName() {
                return scopedClaimType.getSimpleName().toLowerCase(Locale.ROOT) + ".txt";
            }

            @Override
            public String subFolder() {
                return "scopes";
            }
        };
    }


    @Override
    public Map<String, Object> claimsFor(UserId userId, Set<String> requestedScopes) {

        final Map<String, Object> result = new HashMap<>();

        for (var requestedScope : requestedScopes) {
            Optional<Class<? extends ScopedClaims>> scopeClass = this.fromScopeToClass(requestedScope);
            if (scopeClass.isEmpty()) {
                LOGGER.info("Unrecognized scope : " + requestedScope);
                continue;
            }

            try {
                var map = this.fsUserStorage().readMap(userId, this.fsSpecs(scopeClass.get()));
                map.ifPresent(vals -> result.putAll(this.readMap(vals, requestedScope)));

            } catch (IOException e) {
                LOGGER.severe("IOException while reading claims file '" + requestedScope + "' for user with id '" + userId.get() + "'");
            }
        }

        result.remove("userId");
        return result;
    }

    private Map<String, ?> readMap(Map<String, String> vals, String requestedScope) {
        return switch (requestedScope) {
            case "profile" -> this.readProfile(vals);
            case "phone" -> this.readPhone(vals);
            case "email" -> this.readMail(vals);
            case "address" -> this.readAddress(vals);
            default -> Collections.emptyMap();
        };
    }


    private Optional<Class<? extends ScopedClaims>> fromScopeToClass(String requestedScope) {
        return switch (requestedScope) {
            case "profile" -> Optional.of(Profile.class);
            case "phone" -> Optional.of(Phone.class);
            case "email" -> Optional.of(Mail.class);
            case "address" -> Optional.of(Address.class);
            default -> Optional.empty();
        };

    }

    @Override
    public void store(ScopedClaims... someClaims) {

        for (ScopedClaims scoped : someClaims) {
            if (scoped instanceof Phone p) {
                this.store(p);
            } else if (scoped instanceof Profile p) {
                this.store(p);
            } else if (scoped instanceof Mail p) {
                this.store(p);
            } else if (scoped instanceof Address p) {
                this.store(p);
            } else {
                LOGGER.info("Unknown scopedClaim type :" + scoped.getClass().getSimpleName());
            }

        }

    }

    private void store(Address p) {
        try {
            var vals = Map.of(
                    "userId", p.userId().get(),
                    "address", p.address()
            );
            this.fsUserStorage.writeMap(p.userId(), this.fsSpecs(p), vals);
        } catch (IOException e) {
            LOGGER.severe("IOEXCEPTION while writing Address");
        }
    }

    private Map<String, ?> readAddress(Map<String, String> vals) {

        Map<String, Object> result = new HashMap<>();
        this.copyIfPresent("userId", vals, result, UserId::of);
        this.copyIfPresent("address", vals, result);

        return result;
    }

    private void store(Mail p) {
        try {
            var vals = Map.of(
                    "email", p.email(),
                    "userId", p.userId().get(),
                    "email_verified", Boolean.toString(p.email_verified())
            );

            this.fsUserStorage.writeMap(p.userId(), this.fsSpecs(p), vals);
        } catch (IOException e) {
            LOGGER.severe("IOEXCEPTION while writing Address");
        }
    }

    private Map<String, ?> readMail(Map<String, String> vals) {

        Map<String, Object> result = new HashMap<>();
        this.copyIfPresent("userId", vals, result, UserId::of);
        this.copyIfPresent("email", vals, result);
        this.copyIfPresent("email_verified", vals, result, Boolean::parseBoolean);

        return result;
    }

    public void copyIfPresent(String key, Map<String, String> source, Map<String, Object> dest, Function<String, Object> adapter) {
        if (!Utils.isBlank(source.get(key))) {
            dest.put(key, adapter.apply(source.get(key)));
        }
    }

    public void copyIfPresent(String key, Map<String, String> source, Map<String, Object> dest) {
        this.copyIfPresent(key, source, dest, s -> s);
    }

    private Map<String, ?> readProfile(Map<String, String> vals) {
        var result = new HashMap<String, Object>();
        this.copyIfPresent("userId", vals, result, UserId::of);
        this.copyIfPresent("name", vals, result);
        this.copyIfPresent("given_name", vals, result);
        this.copyIfPresent("family_name", vals, result);
        this.copyIfPresent("middle_name", vals, result);
        this.copyIfPresent("nickname", vals, result);
        this.copyIfPresent("preferred_username", vals, result);
        this.copyIfPresent("profile", vals, result);
        this.copyIfPresent("picture", vals, result);
        this.copyIfPresent("website", vals, result);
        this.copyIfPresent("gender", vals, result);
        this.copyIfPresent("birth_date", vals, result);
        this.copyIfPresent("zoneinfo", vals, result);
        this.copyIfPresent("locale", vals, result);
        this.copyIfPresent("updated_at", vals, result, Long::parseLong);

        return result;
    }

    private void store(Profile p) {
        try {

            Map<String, String> vals = new HashMap<>();
            vals.put("userId", p.userId().get());
            vals.put("name", p.name());
            vals.put("given_name", p.given_name());
            vals.put("family_name", p.family_name());
            vals.put("middle_name", p.middle_name());
            vals.put("nickname", p.nickname());
            vals.put("preferred_username", p.preferred_username());
            vals.put("profile", p.profile());
            vals.put("picture", p.picture());
            vals.put("website", p.website());
            vals.put("gender", p.gender());
            vals.put("birth_date", p.birth_date());
            vals.put("zoneinfo", p.zoneinfo());
            vals.put("locale", p.locale());
            vals.put("updated_at", Long.toString(p.updated_at()));

            this.fsUserStorage.writeMap(p.userId(), this.fsSpecs(p), vals);
        } catch (IOException e) {
            LOGGER.severe("IOEXCEPTION while writing Address");
        }
    }

    private void store(Phone p) {
        try {
            var vals = Map.of(
                    "phone", p.phone(),
                    "userId", p.userId().get(),
                    "phone_verified", Boolean.toString(p.phone_verified())
            );
            this.fsUserStorage.writeMap(p.userId(), this.fsSpecs(p), vals);
        } catch (IOException e) {
            LOGGER.severe("IOEXCEPTION while writing Address");
        }
    }

    private Map<String, ?> readPhone(Map<String, String> vals) {

        Map<String, Object> result = new HashMap<>();
        this.copyIfPresent("userId", vals, result, UserId::of);
        this.copyIfPresent("phone", vals, result);
        this.copyIfPresent("phone_verified", vals, result, Boolean::parseBoolean);

        return result;
    }
}