package app.cbo.oidc.java.server.http;


/**
 * FunctionalInterface used to enrich HttpHandler.
 * Allows instanciation using lambdas of classes that only need the path, and not the full handlers (e.g test of ConfigHandler)
 */
@FunctionalInterface
public interface WithPath {

    String path();
}
