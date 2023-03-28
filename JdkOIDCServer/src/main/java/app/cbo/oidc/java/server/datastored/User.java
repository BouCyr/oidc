package app.cbo.oidc.java.server.datastored;

import java.util.*;

public record User(String sub, Map<String, Set<String>> consentedTo) {

    public User(String sub, Map<String, Set<String>> consentedTo) {

        // for readability purpose
        var paramConsentedTo = consentedTo;
        this.sub = sub;
        this.consentedTo = new HashMap<>();
        for(String clientId : paramConsentedTo.keySet()){
            this.consentedTo.put(clientId, new HashSet<>());
            for(String consent : paramConsentedTo.get(clientId)){
                this.consentedTo.get(clientId).add(consent);
            }
        }
    }

    public User(String sub) {
        this(sub, new HashMap<>());
    }

    public boolean hasConsentedTo(String clientId, String scope){
        return this.consentedTo().getOrDefault(clientId, new HashSet<>()).contains(scope);
    }
    public void consentsTo(String clientId, String scope){
        this.consentedTo.computeIfAbsent(clientId, c -> new HashSet<>()).add(scope);
    }



    public boolean hasConsentedToAll(String clientId, List<String> scopes) {
        return this.consentedTo.containsKey(clientId)
                && this.consentedTo.get(clientId).containsAll(scopes);
    }


}
