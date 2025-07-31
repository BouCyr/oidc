package app.cbo.oidc.java.server.webauthn.core;

import java.util.Arrays;
import java.util.Optional;

/**
 * Represents the authenticator data structure.
 * Parsing logic is deferred.
 */
public class AuthenticatorData {

    private final byte[] rpIdHash; //32 bytes
    private final byte flags;
    private final long signCount; // 4 bytes, unsigned big-endian integer
    private final Optional<byte[]> attestedCredentialData; // Optional
    private final Optional<byte[]> extensions; // Optional

    // Raw bytes of the entire authenticator data structure
    private final byte[] rawData;


    /**
     * Constructs an AuthenticatorData object from the raw byte array.
     * The actual parsing of authDataBytes into individual fields will be implemented later.
     * For now, it initializes fields to default/empty values or stores the raw bytes.
     *
     * @param authDataBytes The raw byte array representing authenticator data.
     */
    public AuthenticatorData(byte[] authDataBytes) {
        this.rawData = Arrays.copyOf(authDataBytes, authDataBytes.length);

        // Placeholder parsing - actual parsing logic will be added in the next step
        // For now, just an example of how it might be structured.
        // This will require careful byte-level manipulation.
        if (authDataBytes == null || authDataBytes.length < 37) { // Minimum length: 32 (rpIdHash) + 1 (flags) + 4 (signCount)
            //throw new IllegalArgumentException("Authenticator data byte array is too short or null.");
            // Or initialize with defaults/empty to allow object creation, parsing failure handled later
            this.rpIdHash = new byte[32]; // Placeholder
            this.flags = 0; // Placeholder
            this.signCount = 0; // Placeholder
            this.attestedCredentialData = Optional.empty();
            this.extensions = Optional.empty();
            return;
        }

        // Dummy assignments - actual parsing logic is deferred
        this.rpIdHash = Arrays.copyOfRange(authDataBytes, 0, 32);
        this.flags = authDataBytes[32];
        // This is a simplified way to read a 4-byte unsigned big-endian integer.
        // Proper conversion is needed.
        this.signCount = ((long)(authDataBytes[33] & 0xFF) << 24) |
                         ((long)(authDataBytes[34] & 0xFF) << 16) |
                         ((long)(authDataBytes[35] & 0xFF) << 8)  |
                         ((long)(authDataBytes[36] & 0xFF));

        int currentOffset = 37;

        if (isAttestedCredentialDataPresent()) {
            // This part needs to determine the length of attestedCredentialData correctly
            // AAGUID (16 bytes) + L_credentialId (2 bytes) + credentialId (L_credentialId bytes) + credentialPublicKey (CBOR encoded)
            // This is complex and will be handled in the parsing step.
            // For now, assume it's empty or a fixed dummy length if present.
            // int attestedDataLength = ...; // This is not trivial
            // this.attestedCredentialData = Optional.of(Arrays.copyOfRange(authDataBytes, currentOffset, currentOffset + attestedDataLength));
            // currentOffset += attestedDataLength;
            this.attestedCredentialData = Optional.empty(); // Placeholder until parsing logic
        } else {
            this.attestedCredentialData = Optional.empty();
        }

        if (isExtensionDataPresent()) {
            // The rest of the byte array is extension data (CBOR encoded map)
            //this.extensions = Optional.of(Arrays.copyOfRange(authDataBytes, currentOffset, authDataBytes.length));
             this.extensions = Optional.empty(); // Placeholder until parsing logic
        } else {
            this.extensions = Optional.empty();
        }
    }

    // --- Getters for the parsed fields ---
    public byte[] getRpIdHash() { return Arrays.copyOf(rpIdHash, rpIdHash.length); }
    public byte getFlags() { return flags; }
    public long getSignCount() { return signCount; }
    public Optional<byte[]> getAttestedCredentialData() {
        return attestedCredentialData.map(acd -> Arrays.copyOf(acd, acd.length));
    }
    public Optional<byte[]> getExtensions() {
        return extensions.map(ext -> Arrays.copyOf(ext, ext.length));
    }
    public byte[] getRawData() {
        return Arrays.copyOf(rawData, rawData.length);
    }


    // --- Methods to interpret flags ---

    /** Bit 0: User Present (UP) */
    public boolean isUserPresent() {
        return (flags & 0x01) != 0;
    }

    /** Bit 1: Reserved for future use (RFU1) */
    // No method for RFU1

    /** Bit 2: User Verified (UV) */
    public boolean isUserVerified() {
        return (flags & 0x04) != 0;
    }

    /** Bit 3-5: Reserved for future use (RFU2) */
    // No methods for RFU2

    /** Bit 6: Attested credential data included (AT) */
    public boolean hasAttestedCredentialData() { // Renamed from isAttestedCredentialDataPresent for clarity as per instructions
        return (flags & 0x40) != 0;
    }
    //Internal helper, matching the flag name.
    private boolean isAttestedCredentialDataPresent() {
        return (flags & 0x40) != 0;
    }


    /** Bit 7: Extension data included (ED) */
    public boolean hasExtensionData() { // Renamed from isExtensionDataPresent for clarity
        return (flags & 0x80) != 0;
    }
    //Internal helper, matching the flag name.
    private boolean isExtensionDataPresent() {
        return (flags & 0x80) != 0;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AuthenticatorData that = (AuthenticatorData) o;
        return Arrays.equals(rawData, that.rawData); // Equality based on raw bytes for simplicity now
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(rawData); // Hash based on raw bytes
    }

    @Override
    public String toString() {
        return "AuthenticatorData{" +
               "rpIdHash=" + Arrays.toString(rpIdHash) + // Consider Base64URL for display
               ", flags=" + String.format("0x%02X", flags) +
               ", signCount=" + signCount +
               ", attestedCredentialDataPresent=" + hasAttestedCredentialData() +
               ", extensionDataPresent=" + hasExtensionData() +
               //", attestedCredentialData=" + attestedCredentialData.map(Arrays::toString).orElse("not present") +
               //", extensions=" + extensions.map(Arrays::toString).orElse("not present") +
               '}';
    }
}
