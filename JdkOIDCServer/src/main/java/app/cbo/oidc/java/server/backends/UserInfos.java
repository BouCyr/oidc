package app.cbo.oidc.java.server.backends;

import app.cbo.oidc.java.server.oidc.UserInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UserInfos {

    private static UserInfos instance = null;
    private final Map<String, app.cbo.oidc.java.server.datastored.UserClaims> userClaimsBysub = new ConcurrentHashMap<>();

    private UserInfos() {
    }

    public static UserInfos getInstance() {
        if (instance == null) {
            instance = new UserInfos();
        }
        return instance;
    }

    public UserInfo userInfo(String sub, Set<String> scopes) {

        Map<String, Object> claims = new HashMap<>();

        var userClaims = this.userClaimsBysub.get(sub);


        if (scopes.contains("profile")) {
            claims.putAll(this.profileScope(userClaims));
        }


        /*
        profile OPTIONAL. This scope value requests access to the End-User's default profile Claims, which are: name, family_name, given_name, middle_name, nickname, preferred_username, profile, picture, website, gender, birthdate, zoneinfo, locale, and updated_at.
email
OPTIONAL. This scope value requests access to the email and email_verified Claims.
address
OPTIONAL. This scope value requests access to the address Claim.
phone
OPTIONAL. This scope value requests access to the phone_number and phone_number_verified Claims.
         */


        return new UserInfo(sub, Collections.unmodifiableMap(claims));

    }

    /**
     * profile OPTIONAL. This scope value requests access to the End-User's default profile Claims, which are:
     * name,
     * family_name,
     * given_name,
     * middle_name,
     * nickname,
     * preferred_username,
     * profile,
     * picture,
     * website,
     * gender,
     * birthdate,
     * zoneinfo,
     * locale,
     * and updated_at.
     *
     * @param userClaims source data
     * @return claims from the source data included in profile scope
     */
    private Map<String, Object> profileScope(app.cbo.oidc.java.server.datastored.UserClaims userClaims) {
        Map<String, Object> subClaims = new HashMap<>();
        Optional.ofNullable(userClaims.getName()).ifPresent(n -> subClaims.put("name", n));
        Optional.ofNullable(userClaims.getFamilyName()).ifPresent(n -> subClaims.put("family_name", n));
        Optional.ofNullable(userClaims.getMiddleName()).ifPresent(n -> subClaims.put("middle_name", n));
        Optional.ofNullable(userClaims.getMiddleName()).ifPresent(n -> subClaims.put("nickname", n));
        Optional.ofNullable(userClaims.getMiddleName()).ifPresent(n -> subClaims.put("preferred_username", n));
        Optional.ofNullable(userClaims.getMiddleName()).ifPresent(n -> subClaims.put("profile", n));
        Optional.ofNullable(userClaims.getMiddleName()).ifPresent(n -> subClaims.put("picture", n));
        Optional.ofNullable(userClaims.getMiddleName()).ifPresent(n -> subClaims.put("website", n));
        Optional.ofNullable(userClaims.getMiddleName()).ifPresent(n -> subClaims.put("profile", n));
        Optional.ofNullable(userClaims.getMiddleName()).ifPresent(n -> subClaims.put("gender", n));
        Optional.ofNullable(userClaims.getMiddleName()).ifPresent(n -> subClaims.put("birthdate", n));


        return subClaims;
    }
}
