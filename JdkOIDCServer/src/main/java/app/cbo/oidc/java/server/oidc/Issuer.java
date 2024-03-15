package app.cbo.oidc.java.server.oidc;

import app.cbo.oidc.java.server.scan.BuildWith;
import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.scan.Prop;

@Injectable
public record Issuer(String id) {


    @BuildWith
    public Issuer(@Prop("domain") String domain, @Prop("port") int port) {
        this(domain + ":" + port);
    }

    /**
     * Returns a basic impl of Issuer
     */
    public static Issuer of(String value) {
        return new Issuer(value);
    }

    public String getIssuerId() {
        return id();
    }

}