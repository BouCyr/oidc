package app.cbo.oidc.java.server.scan;

import java.util.Set;
import java.util.function.Function;

/**
 * Functional interface use to specify the class that will scan a package for classes
 * 19/03/2024 : the scanner defined in /main somehow cannot be used from unit tests (something about classloaders) ; Unit Tests use one from a third party lib.
 */
public interface PackageScanner extends Function<String, Set<Class<?>>>{

}
