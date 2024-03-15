package app.cbo.oidc.java.server.scan.sample;

import app.cbo.oidc.java.server.scan.Injectable;

@Injectable
public record AlsoNeedsBackend(NoInterfaceBackend noInterfaceBackend) {
}
