package app.cbo.oidc.java.server.scan.sample;

import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.scan.InstanceOrder;

@Injectable
@InstanceOrder(5)
public record NoDepchild() implements Child {
}
