package app.cbo.oidc.java.server.datastored;

import java.util.*;

public record User(String sub, String pwd, String totpKey, Map<String, Set<String>> consentedTo) {

    public User(String sub,
                String pwd,
                String totpKey,
                Map<String, Set<String>> consentedTo) {

        // for readability purpose
        this.sub = sub;
        this.pwd=pwd;
        this.totpKey=totpKey;

        this.consentedTo = new HashMap<>();
        for(String clientId : consentedTo.keySet()){
            this.consentedTo.put(clientId, new HashSet<>());
            for(String consent : consentedTo.get(clientId)){
                this.consentedTo.get(clientId).add(consent);
            }
        }
    }

    public UserId getUserId(){
        return this::sub;
    }

    public User(String sub, String pwd, String totpKey) {
        this(sub, pwd, totpKey, new HashMap<>());
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
