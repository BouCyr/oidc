package app.cbo.oidc.java.server.scan.sample;

import app.cbo.oidc.java.server.scan.BuildWith;
import app.cbo.oidc.java.server.scan.Injectable;

import java.util.UUID;

@Injectable
public record NoInterfaceBackend(String random) {

    @BuildWith
    public NoInterfaceBackend() {
        this(UUID.randomUUID().toString());
    }
}
