package app.cbo.oidc.java.server.utils;

import java.util.Optional;
import java.util.stream.Stream;

public enum MimeType {
    FORM("application/x-www-form-urlencoded", null),
    TEXT_PLAIN("text/plain", "txt"),
    CSS("text/css", "css"),
    ICO("image/x-icon", "ico"),
    JS("text/javascript", "js"),
    JSON("application/json", "json"),
    JWT("application/jwt", null),
    JWK("application/jwk+json", null),
    JWKSET("application/jwk-set+json", null),
    ;

    private final String mime;
    private final String standardExtension;

    public String mimeType() {
        return mime;
    }

    public boolean hasStandardExtension() {
        return !Utils.isBlank(this.standardExtension);
    }

    MimeType(String mime, String extension) {
        this.mime=mime;
        this.standardExtension = extension;
    }

    public static Optional<MimeType> fromExtension(String extension){
        return  Stream.of(MimeType.values())
                .filter(MimeType::hasStandardExtension)
                .filter(mt -> mt.standardExtension.equals(extension))
                .findFirst();

    }
}
