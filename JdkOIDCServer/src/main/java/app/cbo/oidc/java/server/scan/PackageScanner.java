package app.cbo.oidc.java.server.scan;

import java.util.Set;
import java.util.function.Function;

public interface PackageScanner extends Function<String, Set<Class<?>>>{

}
