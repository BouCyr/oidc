package app.cbo.oidc.java.server.datastored.user.claims;

import app.cbo.oidc.java.server.datastored.user.UserId;

public record Profile(
        UserId userId,
        String name,
        String given_name,
        String family_name,
        String middle_name,
        String nickname,
        String preferred_username,
        String profile, //URL
        String picture, //URL
        String website, //URL
        String gender,
        String birth_date,
        String zoneinfo,
        String locale,
        long updated_at//TODO : handle in Claims backend...

) implements ScopedClaims {
    @Override
    public String scopeName() {
        return "profile";
    }
}
