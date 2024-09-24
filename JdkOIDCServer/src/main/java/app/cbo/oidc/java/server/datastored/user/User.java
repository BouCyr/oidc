package app.cbo.oidc.java.server.datastored.user;

import app.cbo.oidc.java.server.datastored.ClientId;
import app.cbo.oidc.java.server.jsr305.NotNull;
import app.cbo.oidc.java.server.jsr305.Nullable;

import java.util.*;

public record User(String sub, String pwd, String totpKey, Map<String, Set<String>> consentedTo) {

    public User(@NotNull String sub,
                @Nullable String pwd,
                @Nullable String totpKey,
                @Nullable Map<String, Set<String>> consentedTo) {

        // for readability purpose
        this.sub = sub;
        this.pwd = pwd;
        this.totpKey = totpKey;


        this.consentedTo = new HashMap<>();


        for (String clientId : (consentedTo != null ? consentedTo : new HashMap<String, Set<String>>()).keySet()) {
            this.consentedTo.put(clientId, new HashSet<>());
            for (String consent : consentedTo.get(clientId)) {
                this.consentedTo.get(clientId).add(consent);
            }
        }
    }

    public UserId getId(){
        return UserId.of(this.sub());
    }

    public User(@NotNull String sub, @Nullable String pwd, @Nullable String totpKey) {
        this(sub, pwd, totpKey, new HashMap<>());
    }

    public boolean hasConsentedTo(@NotNull ClientId clientId, String scope) {
        return this.consentedTo().getOrDefault(clientId.id(), new HashSet<>()).contains(scope);
    }

    public void consentsTo(@NotNull ClientId clientId, String scope) {
        this.consentedTo.computeIfAbsent(clientId.id(), c -> new HashSet<>()).add(scope);
    }

    public boolean hasConsentedToAll(@NotNull ClientId clientId, List<String> scopes) {
        return this.consentedTo.containsKey(clientId.id())
               && this.consentedTo.get(clientId.id()).containsAll(scopes);
    }


    public Set<String> scopesConsentedTo(@NotNull ClientId clientId) {

        var forThisClient = this.consentedTo().get(clientId.id());
        if (forThisClient == null) {
            return Collections.emptySet();
        } else {
            return Set.copyOf(forThisClient);
        }
    }
}
