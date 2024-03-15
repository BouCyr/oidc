package app.cbo.oidc.java.server.scan.sample;

import app.cbo.oidc.java.server.scan.Injectable;
import app.cbo.oidc.java.server.scan.Prop;

@Injectable
public record SubChildWithProps(@Prop("property") String property,
                                @Prop(value = "withDefault", or = "default") String withDefault,
                                @Prop(value = "otherProperty", or = "default_2") String withDefaultButGiven) implements SubChild {
}
