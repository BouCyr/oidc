package app.cbo.oidc.java.server.datastored.user;

import java.util.List;
import java.util.Objects;

public class WebAuthnCredential {

    private UserId userId; 
    private String credentialId; // Often stored as Base64URL encoded string from byte[]
    private String publicKeyCose; // Often stored as Base64URL encoded string from byte[]
    private long signatureCount;
    private String userHandle; // Often stored as Base64URL encoded string from byte[]
    
    // Optional fields
    private List<String> transports;
    private boolean backedUp;
    private String attestationType;
    private String aaguid;
    private String friendlyName;
    private long registrationTime; //epoch millis

    // Constructors
    public WebAuthnCredential() {}

    public WebAuthnCredential(UserId userId, String credentialId, String publicKeyCose, 
                              long signatureCount, String userHandle, List<String> transports, 
                              boolean backedUp, String attestationType, String aaguid, 
                              String friendlyName, long registrationTime) {
        this.userId = userId;
        this.credentialId = credentialId;
        this.publicKeyCose = publicKeyCose;
        this.signatureCount = signatureCount;
        this.userHandle = userHandle;
        this.transports = transports;
        this.backedUp = backedUp;
        this.attestationType = attestationType;
        this.aaguid = aaguid;
        this.friendlyName = friendlyName;
        this.registrationTime = registrationTime;
    }

    // Getters and Setters
    public UserId getUserId() { return userId; }
    public void setUserId(UserId userId) { this.userId = userId; }

    public String getCredentialId() { return credentialId; }
    public void setCredentialId(String credentialId) { this.credentialId = credentialId; }

    public String getPublicKeyCose() { return publicKeyCose; }
    public void setPublicKeyCose(String publicKeyCose) { this.publicKeyCose = publicKeyCose; }

    public long getSignatureCount() { return signatureCount; }
    public void setSignatureCount(long signatureCount) { this.signatureCount = signatureCount; }

    public String getUserHandle() { return userHandle; }
    public void setUserHandle(String userHandle) { this.userHandle = userHandle; }

    public List<String> getTransports() { return transports; }
    public void setTransports(List<String> transports) { this.transports = transports; }
    
    public boolean isBackedUp() { return backedUp; }
    public void setBackedUp(boolean backedUp) { this.backedUp = backedUp; }

    public String getAttestationType() { return attestationType; }
    public void setAttestationType(String attestationType) { this.attestationType = attestationType; }

    public String getAaguid() { return aaguid; }
    public void setAaguid(String aaguid) { this.aaguid = aaguid; }

    public String getFriendlyName() { return friendlyName; }
    public void setFriendlyName(String friendlyName) { this.friendlyName = friendlyName; }

    public long getRegistrationTime() { return registrationTime; }
    public void setRegistrationTime(long registrationTime) { this.registrationTime = registrationTime; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WebAuthnCredential that = (WebAuthnCredential) o;
        return signatureCount == that.signatureCount &&
               backedUp == that.backedUp &&
               registrationTime == that.registrationTime &&
               Objects.equals(userId, that.userId) &&
               Objects.equals(credentialId, that.credentialId) &&
               Objects.equals(publicKeyCose, that.publicKeyCose) &&
               Objects.equals(userHandle, that.userHandle) &&
               Objects.equals(transports, that.transports) &&
               Objects.equals(attestationType, that.attestationType) &&
               Objects.equals(aaguid, that.aaguid) &&
               Objects.equals(friendlyName, that.friendlyName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, credentialId, publicKeyCose, signatureCount, userHandle, 
                            transports, backedUp, attestationType, aaguid, friendlyName, registrationTime);
    }

    @Override
    public String toString() {
        return "WebAuthnCredential{" +
               "userId=" + userId +
               ", credentialId='" + credentialId + '\'' +
               ", publicKeyCose='" + (publicKeyCose != null ? publicKeyCose.substring(0, Math.min(publicKeyCose.length(), 20)) + "..." : null) + '\'' +
               ", signatureCount=" + signatureCount +
               ", userHandle='" + userHandle + '\'' +
               ", transports=" + transports +
               ", backedUp=" + backedUp +
               ", attestationType='" + attestationType + '\'' +
               ", aaguid='" + aaguid + '\'' +
               ", friendlyName='" + friendlyName + '\'' +
               ", registrationTime=" + registrationTime +
               '}';
    }
}
